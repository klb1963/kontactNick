package kontactNick.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kontactNick.security.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String JWT_COOKIE_NAME = "jwt-token";

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${server.ssl.enabled:false}") // Читаем настройку SSL (true/false)
    private boolean isSecure;

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ✅ Генерирует HTTP-only cookie с JWT-токеном
     */
    public ResponseCookie generateCookie(String token) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .path("/")
                .maxAge(Duration.ofDays(7)) // Устанавливаем куку на 7 дней
                .httpOnly(true)
                .secure(isSecure)
                .sameSite("Lax")
                .build();
    }

    /**
     * ✅ Очищает JWT-cookie (выход из системы)
     */
    public ResponseCookie clearCookie() {
        return ResponseCookie.from(JWT_COOKIE_NAME, "")
                .path("/")
                .maxAge(0) // Сразу удаляем куку
                .httpOnly(true)
                .secure(isSecure)
                .sameSite("Lax")
                .build();
    }

    /**
     * ✅ Извлекает JWT-токен из cookies запроса
     */
    public String extractTokenFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, JWT_COOKIE_NAME);
        if (cookie != null) {
            log.debug("🍪 Extracted token from cookie: {}", cookie.getValue());
            return cookie.getValue();
        }
        log.warn("❌ No JWT token found in cookies.");
        return null;
    }

    /**
     * ✅ Проверяет валидность JWT-токена
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("❌ Token is empty or null.");
            return false;
        }

        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            log.warn("❌ Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}