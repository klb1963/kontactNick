package kontactNick.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        log.debug("JwtAuthenticationFilter: Filtering request: {} {}", request.getMethod(), request.getRequestURI());

        String header = request.getHeader("Authorization");
        log.debug("JwtAuthenticationFilter: Authorization header: {}", header);

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

                // Добавляем роль по умолчанию, если не найдено
                if (role == null) {
                    role = "ROLE_USER";
                    log.warn("JwtAuthenticationFilter: Role is missing in token. Setting default ROLE_USER.");
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("JwtAuthenticationFilter: Setting authentication in SecurityContextHolder");

                    // Создаем UserDetails
                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                            email, "", Collections.singletonList(new SimpleGrantedAuthority(role))
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

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