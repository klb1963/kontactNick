package kontactNick.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        log.info("✅ User registered: {}", userDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        log.debug("🔑 Login request received: email={}", loginDto.getEmail());

        String newToken = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());
        if (!StringUtils.hasText(newToken)) {
            log.warn("❌ Invalid login attempt: {}", loginDto.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid email or password"));
        }

        ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", newToken)
                .path("/")
                .maxAge(Duration.ofDays(7)) // Устанавливаем куку на 7 дней
                .httpOnly(true)
                .secure(false) // true для HTTPS
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        log.info("✅ Login successful, new token issued for {}", loginDto.getEmail());
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    /**
     * ✅ Получение JWT-токена из Cookies
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt-token".equals(cookie.getName())) {
                    String jwt = cookie.getValue();
                    if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                        String email = jwtTokenProvider.getUsernameFromToken(jwt);
                        log.info("✅ Token retrieved for user: {}", email);
                        return ResponseEntity.ok(Map.of("token", jwt));
                    } else {
                        log.warn("❌ Invalid or expired JWT token in cookie");
                    }
                }
            }
        }
        log.debug("🔍 No valid token found in cookies");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token not found"));
    }

    /**
     * ✅ Проверка аутентификации пользователя
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
     * ✅ Получение access_token Google
     */
    @GetMapping("/google-token")
    public ResponseEntity<?> getGoogleToken(HttpServletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("🔍 Запрос Google Access Token для: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().getGoogleAccessToken() == null) {
            log.warn("❌ Google Access Token не найден для {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Google token not found"));
        }

        String accessToken = userOpt.get().getGoogleAccessToken();
        log.info("✅ Возвращаем Google Access Token для {}: {}", email, accessToken);
        return ResponseEntity.ok(Map.of("google_access_token", accessToken));
    }

    /**
     * ✅ Выход из системы (Logout)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        log.info("🔴 Logging out user...");
        ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", "").httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        log.info("✅ Logout successful");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}