package kontactNick.service;

import jakarta.transaction.Transactional;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2AuthenticationService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Transactional
    public String processUserAuthentication(OidcUser oidcUser, OAuth2AuthorizedClient authorizedClient) {
        String email = oidcUser.getEmail();
        log.info("🔍 OAuth User Info: email={}", email);
        log.info("🔍 OIDC User Attributes: {}", oidcUser.getAttributes()); // Логируем атрибуты пользователя

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.warn("⚠️ Пользователь с email {} не найден, создаем нового...", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName((String) oidcUser.getAttribute("name"));
            newUser.setAvatarUrl((String) oidcUser.getAttribute("picture"));
            newUser.setRole(Roles.ROLE_USER);
            return userRepository.save(newUser);
        });

        // 🔹 Проверяем, что `authorizedClient` содержит Access Token
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            log.error("❌ Ошибка: access_token отсутствует у OAuth2AuthorizedClient");
            throw new IllegalStateException("Access token отсутствует.");
        }

        // 🔹 Достаём Access/Refresh токены
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String refreshToken = authorizedClient.getRefreshToken() != null ? authorizedClient.getRefreshToken().getTokenValue() : null;
        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(3600); // 1 час по умолчанию
        }

        // 🔹 Сохраняем Google токены в БД
        user.setGoogleAccessToken(accessToken);
        user.setGoogleRefreshToken(refreshToken);
        user.setGoogleTokenExpiry(expiresAt);

        // 💾 Обновляем пользователя в БД через репозиторий
        userRepository.save(user);
        log.info("✅ Google токены сохранены для {}", email);

        // 🔹 Генерируем JWT-токен и возвращаем
        return jwtTokenProvider.generateToken(user.getEmail(), Roles.ROLE_USER.name());
    }

    public String getValidAccessToken(User user) {
        log.info("🔍 Проверяем access_token для {}", user.getEmail());

        if (user.getGoogleAccessToken() == null) {
            log.error("❌ Нет access_token для {}", user.getEmail());
            throw new IllegalStateException("Access token отсутствует: " + user.getEmail());
        }

        if (user.getGoogleTokenExpiry() == null || Instant.now().isAfter(user.getGoogleTokenExpiry())) {
            log.warn("⚠️ Access token истёк. Обновляем...");
            return refreshAccessToken(user);
        }

        return user.getGoogleAccessToken();
    }

    /**
     * ✅ Обновляет Google Access Token с помощью Refresh Token
     */
    public String refreshAccessToken(User user) {
        log.info("🔄 Обновляем Google access_token для {}", user.getEmail());

        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.error("❌ Ошибка: Refresh token отсутствует для {}", user.getEmail());
            throw new IllegalStateException("Refresh token отсутствует, требуется повторная авторизация");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", user.getGoogleRefreshToken());
        requestBody.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                log.error("❌ Ошибка: ответ от Google не содержит access_token");
                throw new IllegalStateException("Ошибка обновления access_token через refresh_token");
            }

            String newAccessToken = (String) responseBody.get("access_token");
            int expiresIn = (Integer) responseBody.getOrDefault("expires_in", 3600);

            // 🔹 Обновляем пользователя
            Instant expiryTime = Instant.now().plusSeconds(expiresIn);
            log.info("✅ Новый Access Token: {}", newAccessToken);
            log.info("⏳ Новый срок действия (expires_in): {} секунд", expiresIn);
            log.info("⏳ Новый Google Access Token истекает в: {}", expiryTime);

            user.setGoogleAccessToken(newAccessToken);
            user.setGoogleTokenExpiry(Instant.now().plusSeconds(expiresIn)); // Всегда в UTC
            // user.setGoogleExpiresIn(expiresIn); // Для проверки
            userRepository.save(user);

            // 🔄 Проверяем сохранение в БД
            User updatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
            log.info("🔄 Проверяем сохранённое время истечения в БД: {}", updatedUser.getGoogleTokenExpiry());

            return newAccessToken;

        } catch (Exception e) {
            log.error("❌ Исключение при обновлении access_token: {}", e.getMessage());
            throw new IllegalStateException("Ошибка при обновлении access_token", e);
        }
    }


}