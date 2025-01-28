package kontactNick.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
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

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter(TokenService tokenService, JwtTokenProvider jwtTokenProvider) {
        this.tokenService = tokenService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        log.info("🔍 JwtAuthenticationFilter: Incoming request -> {} {}", request.getMethod(), request.getRequestURI());

        String token = null;
        if (request.getCookies() != null) {
            log.debug("🍪 JwtAuthenticationFilter: Checking cookies for JWT token...");
            token = tokenService.extractTokenFromCookies(request);

            if (token != null && jwtTokenProvider.validateToken(token)) { // ✅ Проверяем валидность токена
                log.info("✅ JwtAuthenticationFilter: Valid token found in cookies.");
            } else {
                log.warn("❌ JwtAuthenticationFilter: Invalid or missing token in cookies.");
                token = null; // Очищаем переменную, если токен невалидный
            }
        }

        if (token == null) {
            log.warn("❌ JwtAuthenticationFilter: No valid JWT token found in cookies.");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("✅ JwtAuthenticationFilter: Extracted JWT token: {}", token);

        try {
            log.debug("🔑 JwtAuthenticationFilter: Parsing JWT token...");
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            log.info("✅ JwtAuthenticationFilter: Token successfully parsed. Email: {}, Role: {}", email, role);

            if (role == null) {
                role = "ROLE_USER";
                log.warn("⚠️ JwtAuthenticationFilter: Role is missing in token. Assigning default ROLE_USER.");
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("🔐 JwtAuthenticationFilter: Setting authentication in SecurityContextHolder...");

                // Создаем UserDetails
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        email, "", Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("✅ JwtAuthenticationFilter: SecurityContextHolder updated for user: {}", email);
            } else {
                log.debug("🔄 JwtAuthenticationFilter: SecurityContextHolder already contains authentication: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName());
            }

        } catch (Exception e) {
            log.error("❌ JwtAuthenticationFilter: Token validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}