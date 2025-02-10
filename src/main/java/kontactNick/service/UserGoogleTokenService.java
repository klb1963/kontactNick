package kontactNick.service;

import jakarta.annotation.PostConstruct;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    public UserGoogleTokenService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logGoogleConfig() {
        System.out.println("🔍 Google Client ID: " + clientId);
        System.out.println("🔍 Google Client Secret: " + clientSecret);
    }

    /**
     * Возвращает действительный access_token пользователя.
     * Если токен истёк, выбрасывает исключение, чтобы вызвать повторный логин.
     */
    public String getValidAccessToken(User user) {
        if (user.getGoogleAccessToken() == null) {
            log.error("❌ Ошибка: у пользователя {} нет access token'а!", user.getEmail());
            throw new IllegalStateException("Access token is missing for user: " + user.getEmail());
        }

        if (user.getGoogleTokenExpiry() == null || Instant.now().isAfter(user.getGoogleTokenExpiry())) {
            log.warn("⚠️ Access token для {} истёк! Требуется повторная авторизация.", user.getEmail());
            throw new IllegalStateException("Access token expired. User must re-login: " + user.getEmail());
        }

        return user.getGoogleAccessToken();
    }

}


///*
// Проверяет, истёк ли токен, и обновляет его при необходимости.
//
//public String getValidAccessToken(User user) {
//    if (user.getGoogleTokenExpiry() == null || Instant.now().isAfter(user.getGoogleTokenExpiry())) {
//        return refreshAccessToken(user);
//    }
//    return user.getGoogleAccessToken();
//}
//
///**
// * Обновляет access_token, используя refresh_token.
// */
//public String refreshAccessToken(User user) {
//    if (user.getGoogleRefreshToken() == null) {
//        log.error("❌ Ошибка: у пользователя {} нет refresh token'а в базе!", user.getEmail());
//        throw new IllegalStateException("Refresh token is missing for user: " + user.getEmail());
//    }
//
//    String url = "https://oauth2.googleapis.com/token";
//
//    HttpHeaders headers = new HttpHeaders();
//    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//    Map<String, String> params = new HashMap<>();
//    params.put("client_id", clientId);
//    params.put("client_secret", clientSecret);
//    params.put("refresh_token", user.getGoogleRefreshToken());
//    params.put("grant_type", "refresh_token");
//
//    HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);
//    ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
//
//    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//        String newAccessToken = (String) response.getBody().get("access_token");
//        int expiresIn = (Integer) response.getBody().get("expires_in");
//
//        user.setGoogleAccessToken(newAccessToken);
//        user.setGoogleTokenExpiry(Instant.now().plusSeconds(expiresIn));
//        userRepository.save(user);
//
//        return newAccessToken;
//    }
//    throw new RuntimeException("Failed to refresh access token for user: " + user.getEmail());
//}
