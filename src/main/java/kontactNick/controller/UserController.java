package kontactNick.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/oauth2")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Object user) {
        if (user == null) {
            logger.warn("Попытка доступа к профилю без аутентификации");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (user instanceof OidcUser oidcUser) {
            logger.info("OAuth2 Login Successful: {}", oidcUser.getEmail());
            return ResponseEntity.ok(Map.of(
                    "email", oidcUser.getEmail(),
                    "name", oidcUser.getFullName(),
                    "picture", oidcUser.getPicture(),
                    "attributes", oidcUser.getAttributes() // Отдаем все атрибуты на всякий случай
            ));
        }

        if (user instanceof UserDetails userDetails) {
            logger.info("JWT Login Successful: {}", userDetails.getUsername());
            return ResponseEntity.ok(Map.of("username", userDetails.getUsername()));
        }

        logger.error("Неизвестный тип пользователя: {}", user.getClass().getName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unknown user type");
    }

}
