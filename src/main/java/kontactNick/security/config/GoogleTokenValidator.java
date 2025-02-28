package kontactNick.security.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleTokenValidator {

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isValid(String token) {
        try {
            String url = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + token;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
