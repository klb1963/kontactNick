package kontactNick.controller;

import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/oauth2")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository; // Добавляем репозиторий

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(OAuth2AuthenticationToken authentication) {
        String email = authentication.getPrincipal().getAttribute("email");

        // Получаем пользователя из базы
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();

        // Формируем JSON-ответ с данными из базы
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("nick", user.getNick());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }
}

