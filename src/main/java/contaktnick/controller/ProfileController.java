package contaktnick.controller;

import contaktnick.dto.UserProfileDto;
import contaktnick.entity.User;
import contaktnick.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal OidcUser oidcUser) {
        // Получаем email из авторизации
        String email = oidcUser.getEmail();
        // Ищем пользователя в базе по email
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new UserProfileDto(
                        user.getNick(),
                        user.getEmail(),
                        user.getRole() // Передаём enum Roles
                )))
                .orElse(ResponseEntity.notFound().build());
    }

}
