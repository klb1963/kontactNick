package kontactNick.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
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
        log.info("AuthController initialized"); // Проверяем, вызывается ли конструктор
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("kontactNick.controller").setLevel(Level.DEBUG);
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {

        log.debug("Login request received: email={}, password={}", loginDto.getEmail(), loginDto.getPassword());

        // Аутентификация пользователя
        String token = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        // Если токен пустой, возвращаем ошибку 401
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Invalid email or password"));
        }

        final ResponseCookie accessTokenCookie = tokenService.generateCookie(token);
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // Возвращаем токен в JSON-формате
        return ResponseEntity.ok(Collections.singletonMap("token", token));
    }


    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAuthToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("jwt") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User is not authenticated"));
        }

        String token = (String) session.getAttribute("jwt");

        log.debug("🔑 Returning stored token: {}", token);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public boolean isAuthenticated() {
        return true;
    }

}

