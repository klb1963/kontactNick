package kontactNick.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.User;
import kontactNick.exception_handling.exceptions.DuplicateEmailException;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.OAuth2AuthenticationService;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${github.client-id}")
    private String githubClientId;

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    // ✅ Регистрация пользователя
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        try {
            userService.register(userDto);
            log.info("✅ User registered: {}", userDto.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
        } catch (DuplicateEmailException e) {
            log.warn("❌ Email уже используется: {}", userDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Аутентификация пользователя
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        log.debug("🔑 Login request received: email={}", loginDto.getEmail());

        try {
            String newToken = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

            ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", newToken)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .build();

            response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            log.info("✅ Login successful, new token issued for {}", loginDto.getEmail());

            return ResponseEntity.ok(Map.of("token", newToken));

        } catch (BadCredentialsException e) {
            log.warn("❌ Invalid login attempt: {}", loginDto.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        }
    }

    // ✅ Получение JWT-токена из Cookies
    @GetMapping("/token")
    public ResponseEntity<?> getToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token not found"));
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "jwt-token".equals(cookie.getName()))
                .findFirst()
                .map(cookie -> {
                    String jwt = cookie.getValue();
                    if (jwtTokenProvider.validateToken(jwt)) {
                        String email = jwtTokenProvider.getUsernameFromToken(jwt);
                        log.info("✅ Token retrieved for user: {}", email);
                        return ResponseEntity.ok(Map.of("token", jwt));
                    }
                    log.warn("❌ Invalid or expired JWT token in cookie");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token not found")));
    }

    // ✅ Проверка аутентификации пользователя
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> isAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("❌ Authentication check failed: user is not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        log.info("✅ Authentication check: user is authenticated as {}", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("authenticated", true, "email", userDetails.getUsername()));
    }

    // ✅ Получение access_token Google
    @GetMapping("/google-token")
    public ResponseEntity<?> getGoogleToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("❌ Пользователь не аутентифицирован");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String email = authentication.getName();
        log.info("🔍 Запрос Google Access Token для: {}", email);

        return userRepository.findByEmail(email)
                .filter(user -> user.getGoogleAccessToken() != null)
                .map(user -> {
                    String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
                    log.info("✅ Возвращаем Google Access Token для {}", email);
                    return ResponseEntity.ok(Map.of("google_access_token", accessToken));
                })
                .orElseGet(() -> {
                    log.warn("❌ Google Access Token не найден для {}", email);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Google token not found"));
                });
    }

    // ✅ Выход из системы (Logout)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        log.info("🔴 Logging out user...");

        ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // Очистка SecurityContext (дополнительно)
        SecurityContextHolder.clearContext();

        log.info("✅ Logout successful");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}