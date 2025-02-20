package kontactNick.security.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.*;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey secretKey; // Кешируем ключ для улучшения производительности

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("❌ JWT secret key is missing or too short. Please configure 'jwt.secret' with at least 32 characters.");
        }
        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException("❌ Invalid JWT expiration time. Check 'jwt.expiration-ms' in configuration.");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("✅ JWT secret key initialized successfully. Expiration time: {} ms", jwtExpirationMs);
    }

    /**
     * ✅ Генерирует новый JWT-токен.
     */
    public String generateToken(String email, String role) {
        log.debug("🔐 Generating token for email: {}, role: {}", email, role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString()) // 🔥 Уникальный идентификатор токена
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * ✅ Проверяет валидность JWT токена и логирует причину ошибки, если он недействителен.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("❌ Empty or null token provided for validation.");
            return false;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                log.warn("❌ Token is missing an expiration date.");
                return false;
            }
            if (expiration.before(new Date())) {
                log.warn("❌ Token expired at: {}", expiration);
                return false;
            }

            log.debug("✅ Token is valid. Expires at: {}", expiration);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("❌ Token expired at: {}. Reason: {}",
                    e.getClaims() != null ? e.getClaims().getExpiration() : "unknown",
                    e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("❌ Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("❌ Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("❌ Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("❌ JWT token is empty or invalid: {}", e.getMessage());
        }

        return false;
    }

    /**
     * ✅ Извлекает email (subject) из токена.
     */
    public String getUsernameFromToken(String token) {
        if (!validateToken(token)) {
            log.warn("❌ Attempt to extract username from an invalid token.");
            return null;
        }
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}