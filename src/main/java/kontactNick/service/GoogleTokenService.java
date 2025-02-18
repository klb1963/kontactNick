package kontactNick.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.transaction.Transactional;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleTokenService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GOOGLE_REDIRECT_URI}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public GoogleTokenService(UserRepository userRepository, RestTemplate restTemplate, UserService userService) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * ✅ Генерирует OAuth2 URL для входа через Google.
     */
    public String getAuthUrl() {
        return "https://accounts.google.com/o/oauth2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/contacts https://www.googleapis.com/auth/userinfo.profile"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true";
    }

    /**
     * ✅ Декодирует id_token и возвращает payload с email, именем и аватаром.
     */
    public GoogleIdToken.Payload decodeGoogleIdToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            return googleIdToken != null ? googleIdToken.getPayload() : null;
        } catch (Exception e) {
            log.error("❌ Ошибка при валидации ID токена: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🔄 Обмен authorization_code на access_token + refresh_token
     */
    public Optional<Map<String, String>> exchangeAuthorizationCodeForTokens(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", authorizationCode);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            log.error("❌ Ошибка получения токенов от Google: {}", response);
            return Optional.empty();
        }

        Map<String, String> tokens = response.getBody().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        return Optional.of(tokens);
    }

    /**
     * ✅ Возвращает актуальный access_token пользователя.
     * Если токен истёк, обновляет его через refresh_token.
     */
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
     * 🔄 Обновляет access_token с помощью refresh_token
     */
    private String refreshAccessToken(User user) {
        log.info("🔄 Обновление access_token для {}", user.getEmail());

        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.error("❌ Нет refresh_token для {}", user.getEmail());
            throw new IllegalStateException("Refresh token отсутствует, требуется повторная авторизация: " + user.getEmail());
        }

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("refresh_token", user.getGoogleRefreshToken());
        requestParams.add("grant_type", "refresh_token");

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, new HttpEntity<>(requestParams), new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String newAccessToken = (String) response.getBody().get("access_token");
                int expiresIn = (Integer) response.getBody().getOrDefault("expires_in", 3600);

                user.setGoogleAccessToken(newAccessToken);
                user.setGoogleTokenExpiry(Instant.now().plusSeconds(expiresIn));
                userRepository.save(user);

                log.info("✅ Access token обновлён для {}", user.getEmail());
                return newAccessToken;
            } else {
                log.error("❌ Ошибка обновления access_token: {}", response);
                throw new IllegalStateException("Ошибка обновления access_token через refresh_token");
            }
        } catch (Exception e) {
            log.error("❌ Исключение при обновлении access_token: {}", e.getMessage());
            throw new IllegalStateException("Ошибка при обновлении access_token", e);
        }
    }

    /**
     * ✅ Создаёт или обновляет пользователя в БД
     */
    @Transactional
    public User getOrCreateUser(String email, String nick, String avatarUrl) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(nick);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole(Roles.ROLE_USER);
            log.info("🆕 Новый пользователь зарегистрирован: {}", email);
            return userRepository.save(newUser);
        });
    }

}