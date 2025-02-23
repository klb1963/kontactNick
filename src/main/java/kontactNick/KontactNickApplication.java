package kontactNick;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {
		"kontactNick.security",
		"kontactNick.config",
		"kontactNick.service"
})
public class KontactNickApplication {

	@Value("${GOOGLE_CLIENT_ID:NOT_SET}")
	private String googleClientId;

	@Value("${GOOGLE_CLIENT_SECRET:NOT_SET}")
	private String googleClientSecret;

	@Value("${GOOGLE_REDIRECT_URI:NOT_SET}")
	private String googleRedirectUri;

	public static void main(String[] args) {
		SpringApplication.run(KontactNickApplication.class, args);
	}

	@PostConstruct
	public void logEnvironmentVariables() {
		log.info("🔍 GOOGLE_CLIENT_ID: {}", googleClientId);
		log.info("🔍 GOOGLE_CLIENT_SECRET: {}", googleClientSecret.replaceAll(".", "*")); // скрываем секрет
		log.info("🔍 GOOGLE_REDIRECT_URI: {}", googleRedirectUri);
	}

}
