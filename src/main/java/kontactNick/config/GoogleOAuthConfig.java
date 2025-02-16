package kontactNick.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "google")
@Component
public class GoogleOAuthConfig {

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

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getPeopleApiUrl() {
        return peopleApiUrl;
    }

}
