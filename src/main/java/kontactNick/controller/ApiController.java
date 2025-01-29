package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.dto.FieldDto;
import kontactNick.dto.UserProfileDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserRepository userRepository;

    @Autowired
    public ApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Главная страница
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }

    // Эндпойнт для профиля пользователя
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

    // ✅ Получение OAuth URL с поддержкой Google и GitHub
    @GetMapping("/auth/external-login")
    public ResponseEntity<String> getExternalAuthUrl(@RequestParam(name = "provider", defaultValue = "google") String provider) {
        log.info("🔗 External login requested for provider: {}", provider);

        String authUrl;
        switch (provider.toLowerCase()) {
            case "github":
                authUrl = "https://github.com/login/oauth/authorize?client_id=YOUR_GITHUB_CLIENT_ID&scope=user";
                break;
            case "google":
            default:
                authUrl = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code"
                        + "&client_id=YOUR_GOOGLE_CLIENT_ID"
                        + "&redirect_uri=http://localhost:8080/login/oauth2/code/google"
                        + "&scope=openid%20profile%20email"
                        + "&state=state_value";
                break;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "http://localhost:4200");

        return ResponseEntity.ok().headers(headers).body(authUrl);
    }
}