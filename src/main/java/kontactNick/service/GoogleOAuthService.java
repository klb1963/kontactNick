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

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GOOGLE_REDIRECT_URI}")
    private String redirectUri;

    @Value("${GOOGLE_AUTH_URL}")
    private String authUrl;

    @Value("${GOOGLE_TOKEN_URL}")
    private String tokenUrl;

    @Value("${GOOGLE_PEOPLE_API_URL}")
    private String peopleApiUrl;

    private final RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        System.out.println("✅ GoogleOAuthService bean создан!");
        System.out.println("🔍 Google Client ID: " + clientId);
        System.out.println("🔍 Google Client ID (из System.getenv()): " + System.getenv("GOOGLE_CLIENT_ID"));
        System.out.println("🔍 [System.getenv] GOOGLE_CLIENT_ID: " + System.getenv("GOOGLE_CLIENT_ID"));
        System.out.println("🔍 [System.getProperties] GOOGLE_CLIENT_ID: " + System.getProperty("GOOGLE_CLIENT_ID"));
        System.out.println("🔍 [Spring @Value] GOOGLE_CLIENT_ID: " + clientId);

    }

    /**
     * ✅ Генерация URL для авторизации
     */
    public String getAuthUrl() {
        return authUrl + "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/contacts https://www.googleapis.com/auth/userinfo.profile" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * ✅ Обмен кода аутентификации на `access_token`
     */
    public String exchangeCodeForAccessToken(String authCode) {

        log.info("🔄 Sending request to exchange auth code for access token...");
        log.info("   🔹 Code: {}", authCode);
        log.info("   🔹 Client ID: {}", clientId);
        log.info("   🔹 Client Secret: {}", clientSecret);
        log.info("   🔹 Redirect URI: {}", redirectUri);
        log.info("   🔹 Token URL: {}", tokenUrl);

        log.info("🔄 Exchanging Google auth code for access token...");
        log.info("📢 Sending request with:");
        log.info("   🔹 Code: {}", authCode);
        log.info("   🔹 Client ID: {}", clientId);
        log.info("   🔹 Redirect URI: {}", redirectUri);
        log.info("   🔹 Grant Type: authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "code=" + authCode +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri=" + redirectUri +
                "&grant_type=authorization_code";

        log.info("🔄 Request body: {}", requestBody);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

        log.info("📢 Response from Google: {}", response.getBody());

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
     * ✅ Обновление `access_token` с `refresh_token`
     */
    public String refreshAccessToken(String refreshToken) {
        log.info("🔄 Refreshing Google access token...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token";

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } catch (Exception e) {
                log.error("❌ Error parsing Google token refresh response", e);
            }
        }
        log.error("❌ Failed to refresh access token, response: {}", response.getBody());
        return null;
    }

    /**
     * ✅ Получение информации о пользователе через Google People API
     */
    public GoogleUser getGoogleUserInfo(String accessToken) {
        log.info("🔍 Fetching Google user info...");

        String userInfoUrl = peopleApiUrl + "/userinfo";

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