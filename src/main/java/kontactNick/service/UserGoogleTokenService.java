package kontactNick.service;

import jakarta.annotation.PostConstruct;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserGoogleTokenService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public UserGoogleTokenService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logGoogleConfig() {
        log.info("🔍 Google Client ID: {}", clientId);
        log.info("🔍 Google Redirect URI: {}", redirectUri);
    }

    /**
     * 1️⃣ Получает access_code из URL-параметра после входа пользователя через Google.
     * Этот метод вызывается из `CustomAuthenticationSuccessHandler`
     */
    public void handleGoogleLogin(User user, String authorizationCode) {
        log.info("📥 Получен authorization_code для пользователя {}", user.getEmail());
        exchangeAuthorizationCodeForTokens(user, authorizationCode);
    }

    /**
     * 2️⃣ Обмен access_code на access_token + refresh_token
     */
    private void exchangeAuthorizationCodeForTokens(User user, String authorizationCode) {
        log.info("🔄 Обмен access_code на токены для {}", user.getEmail());

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("code", authorizationCode);
        requestParams.add("grant_type", "authorization_code");
        requestParams.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(requestParams), Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String accessToken = (String) response.getBody().get("access_token");
            String refreshToken = (String) response.getBody().get("refresh_token");
            int expiresIn = (Integer) response.getBody().get("expires_in");

            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleTokenExpiry(Instant.now().plusSeconds(expiresIn));

            userRepository.save(user);
            log.info("✅ Access & Refresh Tokens сохранены для {}", user.getEmail());
        } else {
            log.error("❌ Ошибка получения токенов от Google: {}", response);
            throw new IllegalStateException("Не удалось получить токены Google");
        }
    }

    /**
     * 3️⃣ Возвращает актуальный access_token пользователя.
     * Если токен истёк, обновляет его через refresh_token.
     */
    public String getValidAccessToken(User user) {
        if (user.getGoogleAccessToken() == null) {
            log.error("❌ Нет access_token для {}", user.getEmail());
            throw new IllegalStateException("Access token is missing for user: " + user.getEmail());
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

        if (user.getGoogleRefreshToken() == null) {
            log.error("❌ Нет refresh_token для {}", user.getEmail());
            throw new IllegalStateException("Refresh token отсутствует, требуется повторная авторизация: " + user.getEmail());
        }

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);
        requestParams.add("refresh_token", user.getGoogleRefreshToken());
        requestParams.add("grant_type", "refresh_token");

        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(requestParams), Map.class);

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
    }
}