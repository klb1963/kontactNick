package kontactNick.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kontactNick.dto.GoogleUser;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.GoogleOAuthService;
import kontactNick.service.TokenService;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final GoogleOAuthService googleOAuthService;  // ✅ Сервис для работы с Google OAuth

    /**
     * ✅ Регистрация нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        log.info("✅ User registered: {}", userDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    /**
     * ✅ Аутентификация пользователя и выдача JWT
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDto loginDto,
                                                     HttpServletResponse response) {
        log.debug("🔑 Login request received: email={}", loginDto.getEmail());

        // 🔐 Аутентификация пользователя
        String newToken = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        if (!StringUtils.hasText(newToken)) {
            log.warn("❌ Invalid login attempt: {}", loginDto.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Invalid email or password"));
        }

        // ✅ Создаём и добавляем НОВЫЙ JWT в HttpOnly Cookie
        ResponseCookie accessTokenCookie = tokenService.generateCookie(newToken);
        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        log.info("✅ Login successful, new token issued for {}", loginDto.getEmail());
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    /**
     * ✅ Получение OAuth URL для Google / GitHub
     */
    @GetMapping("/external-login")
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

    /**
     * ✅ Обрабатываем редирект Google OAuth2, сохраняем токен
     */
    @GetMapping("/oauth/google")
    public ResponseEntity<Map<String, String>> handleGoogleOAuth(@RequestParam("code") String authCode) {
        log.info("🔑 Google OAuth callback received, exchanging code for token...");

        // 🔄 Обмениваем `code` на `access_token`
        String googleAccessToken = googleOAuthService.exchangeCodeForAccessToken(authCode);
        if (googleAccessToken == null) {
            log.error("❌ Failed to exchange Google auth code for access token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google authentication"));
        }

        // 🔍 Получаем данные пользователя из Google API
        GoogleUser googleUser = userService.getGoogleUserInfo(googleAccessToken);
        if (googleUser == null) {
            log.warn("❌ Failed to fetch Google user info.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google access token"));
        }

        // ✅ Создаём/обновляем пользователя в системе
        User user = userService.registerOrUpdateGoogleUser(googleUser);

        // ✅ Генерируем наш JWT-токен
        log.info("🚀 Generating new JWT for Google user: {}", user.getEmail());
        String jwtToken = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        // ✅ Сохраняем Google Access Token в базе
        tokenService.storeGoogleAccessToken(user.getEmail(), googleAccessToken);

        // ✅ Отправляем JWT в HttpOnly Cookie
        ResponseCookie accessTokenCookie = tokenService.generateCookie(jwtToken);

        log.info("✅ Google Login Successful: {}", user.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(Map.of("token", jwtToken, "email", user.getEmail(), "role", user.getRole().name()));
    }

    /**
     * ✅ Получение Google Access Token (для фронтенда)
     */
    @GetMapping("/google-token")
    public ResponseEntity<Map<String, String>> getGoogleToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User is not authenticated"));
        }

        String googleToken = tokenService.getGoogleAccessTokenForUser(userDetails.getUsername());
        if (googleToken == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Google token not found"));
        }
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
                        log.info("✅ Retrieved token for user: {}", email);
                        return ResponseEntity.ok(Map.of("token", jwt));
                    }
                }
            }
        }

        log.warn("❌ Token not found in cookies");
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
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")  // ✅ Разрешаем отправку куки с фронта
    public ResponseEntity<?> logout(HttpServletResponse response) {
        log.info("🔴 Logging out user...");

        // Удаляем куку JWT
        ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", "")
                .httpOnly(true)
                .secure(false)  // ✅ Для работы с localhost используем false
                .sameSite("Lax") // ✅ Устанавливаем SameSite для поддержки кросс-доменных запросов
                .path("/")
                .maxAge(0) // ✅ Немедленное удаление куки
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        log.info("✅ Logout successful");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/google-access-token")
    public ResponseEntity<?> getGoogleAccessToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        // 🔍 Получаем токен из базы или сессии пользователя
        String googleToken = tokenService.getGoogleAccessTokenForUser(userDetails.getUsername());
        if (googleToken == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Google Access Token not found"));
        }

        return ResponseEntity.ok(Map.of("accessToken", googleToken));
    }

}