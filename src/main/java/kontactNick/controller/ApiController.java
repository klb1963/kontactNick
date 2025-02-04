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
import kontactNick.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public ApiController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // ✅ Главная страница API
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }

    // ✅ Создание нового пользователя
    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        userRepository.save(user);
        return "User " + user.getNick() + " created!";
    }

    // ✅ Получение пользователя по email
    @GetMapping("/users/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.of(userRepository.findByEmail(email));
    }

    /**
     * ✅ Получение профиля текущего пользователя
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("❌ Пользователь не аутентифицирован");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String email = userDetails.getUsername();

        if (email == null || email.isBlank()) {
            log.error("❌ Ошибка: у аутентифицированного пользователя отсутствует email!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Email is missing"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("❌ Пользователь с email {} не найден в базе данных", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        log.info("📌 User details: email={}, nick={}, avatar={}, role={}",
                user.getEmail(), user.getNick(), user.getAvatarUrl(), user.getRole());

        log.info("✅ Профиль пользователя загружен: {}", email);
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "nick", user.getNick() != null ? user.getNick() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "role", user.getRole() != null ? user.getRole() : "USER"
        ));
    }

    // ✅ Проверка доступности nick
    @GetMapping("/check-nick")
    public ResponseEntity<?> checkNickAvailability(@RequestParam String nick) {
        boolean isAvailable = userRepository.findByNick(nick).isEmpty();
        log.info("🔍 Проверка nick '{}': {}", nick, isAvailable ? "доступен" : "занят");
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }

    // ✅ Обновление nick текущего пользователя
    @PutMapping("/profile/nick")
    public ResponseEntity<?> updateNick(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, String> request) {
        String newNick = request.get("nick");

        if (newNick == null || newNick.isBlank()) {
            log.warn("❌ Попытка сохранить пустой nick");
            return ResponseEntity.badRequest().body(Map.of("error", "Nick cannot be empty"));
        }

        if (userRepository.findByNick(newNick).isPresent()) {
            log.warn("❌ Nick '{}' уже занят", newNick);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Nick already taken"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setNick(newNick);
        userRepository.save(user);

        log.info("✅ Nick пользователя '{}' обновлен на '{}'", user.getEmail(), newNick);
        return ResponseEntity.ok(Map.of("message", "Nick updated successfully"));
    }

}