package contaktnick.controller;

import contaktnick.dto.UserProfileDto;
import contaktnick.entity.User;
import contaktnick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserRepository userRepository;

    @Autowired
    public ApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Главная страница API
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }

    // Эндпоинт для профиля пользователя
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new UserProfileDto(
                        user.getNick(),
                        user.getEmail(),
                        user.getRole()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // Создание нового пользователя
    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        userRepository.save(user);
        return "User " + user.getNick() + " created!";
    }

    // Получение пользователя по email
    @GetMapping("/users/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.of(userRepository.findByEmail(email));
    }

    // Страница входа (опционально, если нужна)
    @GetMapping("/login")
    public String login() {
        return "login"; // Отобразите страницу входа, если используете шаблоны
    }
}