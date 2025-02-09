package kontactNick.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kontactNick.dto.GoogleUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate; // ✅ Теперь Spring сам инжектирует

    @PostConstruct
    public void init() {
        System.out.println("✅ GoogleOAuthService bean создан!");
        System.out.println("🔍 Google Client ID: " + clientId);
    }

    /**
     * ✅ Обмен кода аутентификации на `access_token`
     */
    public String exchangeCodeForAccessToken(String authCode) {
        log.info("🔄 Exchanging Google auth code for access token...");

        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "code=" + authCode +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri=" + redirectUri +
                "&grant_type=authorization_code";

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } catch (Exception e) {
                log.error("❌ Error parsing Google token response", e);
            }
        }
        log.error("❌ Failed to exchange auth code for token, response: {}", response.getBody());
        return null;
    }

    /**
     * ✅ Получение информации о пользователе через Google People API
     */
    public GoogleUser getGoogleUserInfo(String accessToken) {
        log.info("🔍 Fetching Google user info...");

        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                GoogleUser user = new GoogleUser();
                user.setEmail(jsonNode.get("email").asText());
                user.setNick(jsonNode.get("name").asText());
                user.setPicture(jsonNode.get("picture").asText());
                return user;
            } catch (Exception e) {
                log.error("❌ Error parsing Google user info response", e);
            }
        }
        log.error("❌ Failed to fetch Google user info, response: {}", response.getBody());
        return null;
    }
}