package kontactNick.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

//🔥 Что улучшено?
//
// ✅ Логируется отсутствие cookies
//✅ Добавлен catch для ExpiredJwtException (чтобы отличать истекший токен)
//✅ Разделены ошибки валидации и парсинга токена
//✅ Логируется существующая аутентификация в SecurityContextHolder
//✅ Чёткие и понятные логи на каждом этапе


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

        log.info("🔍 Incoming Authorization header: {}", request.getHeader("Authorization"));
        log.info("🔍 [JwtAuthenticationFilter] Checking authentication...");
        log.info("🔍 JwtAuthenticationFilter: Incoming request -> {} {}", request.getMethod(), request.getRequestURI());

        String token = null;

        if (request.getCookies() == null) {
            log.warn("🍪 JwtAuthenticationFilter: No cookies found in request.");
        } else {
            log.debug("🍪 JwtAuthenticationFilter: Checking cookies for JWT token...");
            token = tokenService.extractTokenFromCookies(request);

            if (token != null) {
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        log.info("✅ JwtAuthenticationFilter: Valid token found in cookies.");
                    } else {
                        log.warn("❌ JwtAuthenticationFilter: Token validation failed.");
                        token = null;
                    }
                } catch (Exception e) {
                    log.error("❌ JwtAuthenticationFilter: Token validation error: {}", e.getMessage());
                    token = null;
                }
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
                log.debug("🔐 JwtAuthenticationFilter: No existing authentication found, setting new authentication...");

                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        email, "", Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("✅ JwtAuthenticationFilter: SecurityContextHolder updated for user: {}", email);
            } else {
                log.debug("🔄 JwtAuthenticationFilter: Authentication already exists in SecurityContextHolder -> {}",
                        SecurityContextHolder.getContext().getAuthentication().getName());
            }

        } catch (ExpiredJwtException e) {
            log.error("❌ JwtAuthenticationFilter: Token has expired: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token expired");
            return;
        } catch (Exception e) {
            log.error("❌ JwtAuthenticationFilter: Token parsing error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}