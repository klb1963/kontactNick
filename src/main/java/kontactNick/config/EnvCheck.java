package kontactNick.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvCheck {

    @Value("${GOOGLE_CLIENT_ID:NOT_SET}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:NOT_SET}")
    private String googleClientSecret;

    @Value("${GOOGLE_REDIRECT_URI:NOT_SET}")
    private String googleRedirectUri;

    @PostConstruct
    public void printEnvVars() {
        System.out.println("GOOGLE_CLIENT_ID: " + googleClientId);
        System.out.println("GOOGLE_CLIENT_SECRET: " + googleClientSecret);
        System.out.println("GOOGLE_REDIRECT_URI: " + googleRedirectUri);
    }
}
