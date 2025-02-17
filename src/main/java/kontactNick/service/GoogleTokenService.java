package kontactNick.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
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
import java.util.Collections;
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

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public GoogleTokenService(UserRepository userRepository, RestTemplate restTemplate) {
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
                + "&include_granted_scopes=true"; // Добавляем этот параметр для получения refresh_token;
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
     * ✅ Создаёт или возвращает пользователя по email.
     */
    public User getOrCreateUserByEmail(String email, String name, String picture) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(name);
            newUser.setAvatarUrl(picture);
            newUser.setRole(Roles.ROLE_USER);
            log.info("🆕 Новый пользователь зарегистрирован: {}", email);
            return userRepository.save(newUser);
        });
    }

    /**
     * 🔄 Обмен authorization_code на access_token + refresh_token
     */
    public Optional<Map<String, String>> exchangeAuthorizationCodeForTokens(String authorizationCode) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", authorizationCode);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Логируем полный ответ от Google
        log.info("🔍 Google OAuth Response: {}", responseEntity.getBody());

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            log.error("❌ Ошибка получения токенов от Google: {}", responseEntity.getBody());
            return Optional.empty();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> tokens = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            return Optional.of(tokens);
        } catch (JsonProcessingException e) {
            log.error("❌ Ошибка обработки JSON-ответа Google", e);
            return Optional.empty();
        }
    }

    /**
     * ✅ Возвращает актуальный access_token пользователя.
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
     * 🔄 Обновляет access_token с помощью refresh_token
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

}