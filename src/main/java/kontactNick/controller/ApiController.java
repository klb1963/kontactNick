package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.dto.FieldDto;
import kontactNick.dto.UserDto;
import kontactNick.dto.UserProfileDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.exception_handling.exceptions.DuplicateEmailException;
import kontactNick.exception_handling.exceptions.NickAlreadyTakenException;
import kontactNick.exception_handling.exceptions.UserNotFoundException;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;

    // ✅ Главная страница API
    @GetMapping("/")
    public ResponseEntity<String> apiRoot() {
        return ResponseEntity.ok("Welcome to KontactNick API");
    }

    // ✅ Создание нового пользователя
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto) {
        try {
            userService.register(userDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User created successfully",
                    "email", userDto.getEmail(),
                    "nick", userDto.getNick() != null ? userDto.getNick() : userDto.getEmail()
            ));
        } catch (DuplicateEmailException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Получение пользователя по email
    @GetMapping("/users/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        if (!email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }
        try {
            User user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Получение профиля текущего пользователя
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "nick", user.getNick(),
                    "avatarUrl", user.getAvatarUrl(),
                    "role", user.getRole()
            ));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Проверка доступности никнейма
    @GetMapping("/check-nick")
    public ResponseEntity<?> checkNickAvailability(@RequestParam String nick) {
        boolean isAvailable = userService.isNickAvailable(nick);
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }

    // ✅ Обновление никнейма текущего пользователя
    @PutMapping("/profile/nick")
    public ResponseEntity<?> updateNick(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, String> request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String newNick = request.get("nick");
        try {
            userService.updateNick(userDetails.getUsername(), newNick);
            return ResponseEntity.ok(Map.of("message", "Nick updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (NickAlreadyTakenException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}