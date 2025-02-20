package kontactNick.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.GoogleTokenService;
import kontactNick.service.TokenService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final GoogleTokenService googleTokenService;

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

        ResponseCookie accessTokenCookie = tokenService.generateCookie(newToken);
        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        log.info("✅ Login successful, new token issued for {}", loginDto.getEmail());
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    /**
     * ✅ Получение OAuth URL (Google/GitHub)
     */
    @GetMapping("/external-login")
    public ResponseEntity<String> getExternalAuthUrl(@RequestParam(name = "provider", defaultValue = "google") String provider) {
        log.info("🔗 External login requested for provider: {}", provider);
        String authUrl = provider.equalsIgnoreCase("github") ?
                "https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&scope=user" :
                googleTokenService.getAuthUrl();

        return ResponseEntity.ok(authUrl);
    }

    /**
     * ✅ Обмен access_code на токены (access и refresh) от Google
     */
    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam("code") String authorizationCode) {
        log.info("📥 Получен authorization_code: {}", authorizationCode);

        Optional<Map<String, String>> tokensOptional = googleTokenService.exchangeAuthorizationCodeForTokens(authorizationCode);

        if (tokensOptional.isEmpty()) {
            log.error("❌ Ошибка при обмене authorization_code на токены");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка авторизации через Google");
        }

        Map<String, String> tokens = tokensOptional.get();
        String idToken = tokens.get("id_token");

        if (idToken == null) {
            log.error("❌ Ошибка: ID токен отсутствует в ответе Google");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка получения ID токена");
        }

        // 🔥 Декодируем ID токен
        GoogleIdToken.Payload payload = googleTokenService.decodeGoogleIdToken(idToken);
        if (payload == null) {
            log.error("❌ Ошибка: не удалось декодировать ID токен");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка валидации ID токена");
        }

        // 🆕 Обновляем данные пользователя
        User user = userService.getOrCreateUser(payload.getEmail(), (String) payload.get("name"), (String) payload.get("picture"));

        user.setGoogleAccessToken(tokens.get("access_token"));
        user.setGoogleRefreshToken(tokens.get("refresh_token"));
        user.setGoogleTokenExpiry(Instant.now().plusSeconds(Integer.parseInt(tokens.get("expires_in"))));
        userService.updateUser(user);

        log.info("✅ Данные пользователя обновлены: {}", user.getEmail());

        return ResponseEntity.ok("Токены успешно сохранены");
    }

    /**
     * ✅ Получение Google Access Token
     */
    @GetMapping("/google-token")
    public ResponseEntity<Map<String, String>> getGoogleToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("❌ User is not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User is not authenticated"));
        }
        String googleToken = tokenService.getGoogleAccessTokenForUser(userDetails.getUsername());
        if (googleToken == null) {
            log.warn("❌ Google token not found for user {}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Google token not found"));
        }
        log.info("✅ Retrieved Google token for user {}", userDetails.getUsername());
        return ResponseEntity.ok(Map.of("googleAccessToken", googleToken));
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
                    if (jwt != null && tokenService.validateToken(jwt)) {
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