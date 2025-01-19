package kontactNick.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret; // Используем секретный ключ из конфигурации

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        log.debug("JwtAuthenticationFilter: Filtering request: {} {}", request.getMethod(), request.getRequestURI());

        String header = request.getHeader("Authorization");

        log.debug("JwtAuthenticationFilter: Incoming request to {} {}", request.getMethod(), request.getRequestURI());

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Убираем "Bearer "
            log.debug("JwtAuthenticationFilter: Extracted token: {}", token);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                log.debug("JwtAuthenticationFilter: Token parsed. Email: {}, Role: {}", email, role);

                // Проверяем, что SecurityContextHolder пуст, чтобы не перезаписывать
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("JwtAuthenticationFilter: Setting authentication in SecurityContextHolder");

                    // Создаем аутентификацию пользователя
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email, null, Collections.singleton(new SimpleGrantedAuthority(role)));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                log.error("JwtAuthenticationFilter: Token validation failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        } else {
            log.debug("JwtAuthenticationFilter: No valid Authorization header found");
        }

        filterChain.doFilter(request, response);
    }
}