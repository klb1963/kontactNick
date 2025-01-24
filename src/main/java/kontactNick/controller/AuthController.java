package kontactNick.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.User;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
import kontactNick.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, TokenService tokenService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
        log.info("✅ AuthController initialized");
    }

    /**
     * Регистрация нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        log.info("✅ User registered: {}", userDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    /**
     * Аутентификация пользователя и выдача JWT
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        log.debug("🔑 Login request received: email={}", loginDto.getEmail());

        String token = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        if (!StringUtils.hasText(token)) {
            log.warn("❌ Invalid login attempt: {}", loginDto.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Invalid email or password"));
        }

        ResponseCookie accessTokenCookie = tokenService.generateCookie(token);
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        log.info("✅ Login successful, token issued for {}", loginDto.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * Получение JWT-токена из Cookies
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAuthToken(@CookieValue(name = "jwt-token", required = false) String token) {
        log.info("🔍 Запрос на /api/auth/token получен");

        if (!StringUtils.hasText(token)) {
            log.warn("❌ JWT не найден в Cookies.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User is not authenticated"));
        }

        log.info("✅ JWT найден в Cookies: {}", token);
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * Проверка аутентификации пользователя
     */
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> isAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("❌ Authentication check failed: user is not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", "false"));
        }
        log.info("✅ Authentication check: user is authenticated as {}", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("authenticated", "true", "email", userDetails.getUsername()));
    }

    /**
     * Получение профиля текущего пользователя
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("❌ Пользователь не аутентифицирован");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String email = userDetails.getUsername();
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("✅ Профиль пользователя загружен: {}", email);
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "nick", user.getNick(),
                "avatarUrl", user.getAvatarUrl(),
                "role", user.getRole()
        ));
    }
}