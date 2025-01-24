package kontactNick.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kontactNick.entity.Roles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}") // ✅ Значение по умолчанию (1 день)
    private long jwtExpirationMs;

    private SecretKey secretKey; // ✅ Кешируем ключ, чтобы не конвертировать его каждый раз

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("❌ JWT secret key is missing or too short. Please configure 'jwt.secret' with at least 32 characters.");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("✅ JWT secret key initialized successfully.");
    }

    public String generateToken(String email, Roles role) {
        log.debug("Generating token for email: {}, role: {}", email, role);

        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(email)
                .claim("role", role.toString()) // ✅ Используем просто toString()
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS512) // ✅ Используем кешированный ключ
                .compact();

        log.debug("✅ Generated token: {} (expires at: {})", token, expiration);
        return token;
    }
}