package kontactNick.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.service.CustomOidcUserService;
import kontactNick.service.OAuth2AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/api/auth/**",
            "/api/public/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/login/oauth2/code/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/api/categories/*/google-resource-name" // 👈 Добавляем эндпоинт
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    private final Environment environment;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔄 [SecurityConfig] Настраиваем Spring Security...");

        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers("/", "/home").permitAll()
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/auth/google-token").authenticated() // Доступ только с JWT
                        .requestMatchers("/api/categories/**").hasAuthority("ROLE_USER")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(this::onAuthenticationSuccess)
                        .failureHandler(customFailureHandler())
                )
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("❌ Unauthorized request to {}", request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
                            response.setHeader("Access-Control-Allow-Credentials", "true");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    /**
     * ✅ Обработчик успешной аутентификации через Google OAuth2
     */
    private void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("✅ Успешная аутентификация: {}", authentication.getName());
        log.info("🔍 Authentication Principal Class: {}", authentication.getPrincipal().getClass().getName());

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            log.info("🔍 OIDC User: {}", oidcUser.getAttributes());

            // 🔹 Получаем OAuth2AuthorizedClient
            String clientRegistrationId = "google"; // Используем Google OAuth2
            String principalName = authentication.getName(); // Обычно email или sub
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.error("❌ Ошибка: не удалось получить OAuth2AuthorizedClient или Access Token отсутствует");
                response.sendRedirect("http://localhost:4200/login?error=oauth_client_error");
                return;
            }

            log.info("🔎 Проверяем OAuth2AuthorizedClient: {}", authorizedClient);
            log.info("🔎 Access Token: {}", authorizedClient.getAccessToken().getTokenValue());
            log.info("🔎 Refresh Token: {}",
                    authorizedClient.getRefreshToken() != null ? authorizedClient.getRefreshToken().getTokenValue() : "null");

            // 🔹 Сохраняем пользователя в БД и получаем JWT
            log.info("🔄 Передаём пользователя в processUserAuthentication: {}", oidcUser.getEmail());
            String jwtToken = null;

            // в методе processUserAuthentication
            // пользователя сохраняем в БД и все его токены из Google
            // пользователю генерим jwtToken вызвав wtTokenProvider

            try {
                log.info("🔄 Перед вызовом processUserAuthentication...");
                jwtToken = oAuth2AuthenticationService.processUserAuthentication(oidcUser, authorizedClient);
                log.info("✅ JWT-токен успешно сгенерирован!");
            } catch (Exception e) {
                log.error("❌ Ошибка в processUserAuthentication: {}", e.getMessage(), e);
                response.sendRedirect("http://localhost:4200/login?error=auth_processing_failed");
                return;
            }

            // 🔹 Проверяем, что jwtToken не null
            if (jwtToken == null || jwtToken.isEmpty()) {
                log.error("❌ Ошибка: JWT-токен не был сгенерирован!");
                response.sendRedirect("http://localhost:4200/login?error=jwt_generation_failed");
                return;
            }

            // 🔹 Устанавливаем JWT в cookie
            ResponseCookie jwtCookie = ResponseCookie.from("jwt-token", jwtToken)
                    .httpOnly(true)
                    .secure(false) // true для HTTPS
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

            log.info("✅ JWT-токен создан и передан пользователю {}", oidcUser.getEmail());

            // 🔹 Перенаправляем пользователя на фронтенд
            response.sendRedirect("http://localhost:4200/dashboard");
        } else {
            log.error("❌ Ошибка: пользователь не является OIDC пользователем (Class: {})",
                    authentication.getPrincipal().getClass().getName());
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }

    @Bean
    public AuthenticationFailureHandler customFailureHandler() {
        return (request, response, exception) -> {
            log.error("❌ Authentication failed: {}", exception.getMessage());
            if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
                log.error("📢 OAuth2 Error Code: {}", oauth2Exception.getError().getErrorCode());
                log.error("📢 OAuth2 Description: {}", oauth2Exception.getError().getDescription());
            }
            response.sendRedirect("/login?error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}