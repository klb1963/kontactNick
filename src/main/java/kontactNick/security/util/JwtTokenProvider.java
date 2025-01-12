package kontactNick.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String generateToken(String email, String role) {
        String token = Jwts.builder()
                .setSubject(email) // В качестве subject передается email
                .claim("role", role) // Добавляем роль в payload
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // Токен действителен 1 день
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

        // Логируем сгенерированный токен (для отладки)
        log.debug("Generated token for email: {} with role: {}", email, role);

        return token;
    }
}
