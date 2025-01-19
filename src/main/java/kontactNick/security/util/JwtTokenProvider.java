package kontactNick.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String generateToken(String email, String role) {
        // Логируем входные параметры
        log.debug("Generating token for email: {}, role: {}", email, role);

        // Проверяем, что jwtSecret не пуст и имеет достаточную длину
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret key is too short or not configured properly.");
        }

        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(email) // В качестве subject передается email
                .claim("role", role) // Добавляем роль в payload
                .setIssuedAt(issuedAt)
                .setExpiration(expiration) // Токен действителен 1 день
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();

        // Логируем сгенерированный токен и дату истечения
        log.debug("Generated token: {} (expires at: {})", token, expiration);

        return token;
    }
}
