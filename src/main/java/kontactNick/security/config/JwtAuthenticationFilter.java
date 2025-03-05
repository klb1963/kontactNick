package kontactNick.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenValidator googleTokenValidator;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter(TokenService tokenService, JwtTokenProvider jwtTokenProvider, GoogleTokenValidator googleTokenValidator) {
        this.tokenService = tokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleTokenValidator = googleTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        log.info("🔍 Incoming Authorization header: {}", request.getHeader("Authorization"));
        log.info("🔍 [JwtAuthenticationFilter] Checking authentication...");
        log.info("🔍 JwtAuthenticationFilter: Incoming request -> {} {}", request.getMethod(), request.getRequestURI());

        String token = null;
        String authHeader = request.getHeader("Authorization");

        // 1️⃣ Проверяем заголовок Authorization (Google Access Token или JWT)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            if (token.startsWith("ya29.")) {
                log.info("🔍 Detected Google Access Token, validating...");
                if (googleTokenValidator.isValid(token)) {
                    log.info("✅ Google Access Token is valid!");
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(token, null, new ArrayList<>())
                    );
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    log.error("❌ Invalid Google Access Token!");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid Google Token");
                    return;
                }
            }
        }

        // ✅ Если токен уже найден в Authorization и это НЕ Google Access Token, пропускаем проверку в cookies
        boolean tokenAlreadyChecked = (token != null && !token.startsWith("ya29."));

        // 2️⃣ Проверяем JWT в куках (если его еще нет)
        if (!tokenAlreadyChecked && request.getCookies() != null) {
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
            log.warn("❌ JwtAuthenticationFilter: No valid JWT token found.");
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

                // ✅ Создаём реальный объект User (а не просто email)
                User user = new User();
                user.setEmail(email);
                user.setRole(Roles.valueOf(role)); // 👈 Преобразуем строку в Enum

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔐 SecurityContextHolder now contains: {}", (auth != null) ? auth.getName() : "null");

        filterChain.doFilter(request, response);
    }
}