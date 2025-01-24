package kontactNick.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
public class TokenService {

    private static final String JWT_COOKIE_NAME = "jwt-token";

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${server.ssl.enabled:false}")  // Читаем настройку SSL (true/false)
    private boolean isSecure;

    public ResponseCookie generateCookie(String value) {
        return ResponseCookie
                .from(JWT_COOKIE_NAME, value)
                .path("/")
                .maxAge(jwtExpirationMs)
                .httpOnly(true)
                .secure(isSecure) // ✅ Добавлено
                .build();
    }

    public String getCookieValueByName(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, JWT_COOKIE_NAME);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    public ResponseCookie clearCookie() {
        return ResponseCookie
                .from(JWT_COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(isSecure)  // ✅ Исправлено: теперь удаление куки корректно работает в HTTPS
                .build();
    }

}
