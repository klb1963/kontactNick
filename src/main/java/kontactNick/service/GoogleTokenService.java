package kontactNick.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
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
import java.util.Map;
import java.util.Optional;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import static org.springframework.security.config.Elements.JWT;

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

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public GoogleTokenService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logGoogleConfig() {
        log.info("🔍 Google Client ID: {}", clientId);
        log.info("🔍 Google Redirect URI: {}", redirectUri);
    }

    /**
     * ✅ Генерация OAuth URL
     */
    public String getAuthUrl() {
        return AUTH_URL + "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/contacts https://www.googleapis.com/auth/userinfo.profile" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * 1️⃣ Получает authorization_code, обменивает его на токены и обновляет пользователя
     */
    public void handleGoogleLogin(String authorizationCode) {
        log.info("📥 Получен authorization_code: {}", authorizationCode);

        // 2️⃣ Обмен кода на токены
        Optional<Map<String, String>> tokensOpt = exchangeAuthorizationCodeForTokens(authorizationCode);
        if (tokensOpt.isEmpty() || !tokensOpt.get().containsKey("id_token")) {
            log.error("❌ Ошибка обмена кода на токены!");
            throw new IllegalStateException("Ошибка получения токенов Google");
        }

        Map<String, String> tokens = tokensOpt.get();
        String idToken = tokens.get("id_token");

        // 3️⃣ Декодируем id_token для получения email пользователя
        String email = JwtUtils.extractEmailFromIdToken(idToken);
        // System.out.println("📧 Email пользователя: " + email);

        if (email == null || email.isEmpty()) {
            log.error("❌ Ошибка: не удалось получить email из id_token!");
            throw new IllegalStateException("Не удалось получить email из id_token");
        }

        // 4️⃣ Проверяем, есть ли пользователь в базе
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(email.substring(0, email.indexOf("@")));
            newUser.setRole(Roles.ROLE_USER);
            log.info("🆕 Новый пользователь зарегистрирован: {}", email);
            return userRepository.save(newUser);
        });

        // 5️⃣ Сохраняем полученные токены в БД
        user.setGoogleAccessToken(tokens.get("access_token"));
        user.setGoogleTokenExpiry(Instant.now().plusSeconds(3600)); // 1 час
        user.setGoogleRefreshToken(tokens.getOrDefault("refresh_token", ""));

        userRepository.save(user);
        log.info("✅ Токены сохранены для пользователя: {}", user.getEmail());
    }

    /**
     * 2️⃣ Обмен authorization_code на access_token + refresh_token
     */
    public Optional<Map<String, String>> exchangeAuthorizationCodeForTokens(String authorizationCode) {
        log.info("🔄 Обмен access_code на токены");

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("code", authorizationCode);
        requestParams.add("grant_type", "authorization_code");
        requestParams.add("redirect_uri", redirectUri);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, new HttpEntity<>(requestParams), new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ Успешно получены токены от Google!");
                return Optional.of(Map.of(
                        "access_token", (String) response.getBody().get("access_token"),
                        "refresh_token", (String) response.getBody().getOrDefault("refresh_token", ""),
                        "id_token", (String) response.getBody().get("id_token")
                ));
            } else {
                log.error("❌ Ошибка получения токенов от Google: {}", response);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("❌ Исключение при обмене кода на токены: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 3️⃣ Возвращает актуальный access_token пользователя.
     * Если токен истёк, обновляет его через refresh_token.
     */
    public String getValidAccessToken(User user) {
        log.info("🔍 Проверяем актуальный access_token для {}", user.getEmail());

        if (user.getGoogleAccessToken() == null) {
            log.error("❌ Нет access_token для {}", user.getEmail());
            throw new IllegalStateException("Access token отсутствует: " + user.getEmail());
        }

        if (user.getGoogleTokenExpiry() == null || Instant.now().isAfter(user.getGoogleTokenExpiry())) {
            log.warn("⚠️ Access token для {} истёк. Обновляем...", user.getEmail());
            return refreshAccessToken(user);
        }

        return user.getGoogleAccessToken();
    }

    /**
     * 4️⃣ Обновляет access_token с помощью refresh_token
     */
    private String refreshAccessToken(User user) {
        log.info("🔄 Обновление access_token через refresh_token для {}", user.getEmail());

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
                int expiresIn = (Integer) response.getBody().get("expires_in");

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
     * ✅ Запрос refresh_token от Google API, используя access_token
     */
    public String fetchRefreshToken(String accessToken) {
        log.info("🔄 Получаем refresh_token через access_token...");

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("grant_type", "refresh_token");
        requestParams.add("access_token", accessToken);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, new HttpEntity<>(requestParams), new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ Refresh token успешно получен!");
                return (String) response.getBody().getOrDefault("refresh_token", "");
            } else {
                log.error("❌ Ошибка получения refresh_token: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Исключение при запросе refresh_token: {}", e.getMessage());
            return null;
        }
    }
}