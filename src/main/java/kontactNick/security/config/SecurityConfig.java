package kontactNick.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.security.config.JwtAuthenticationFilter;
import kontactNick.service.CustomOidcUserService;
import kontactNick.service.OAuth2AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔄 [SecurityConfig] Настраиваем Spring Security...");

        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/home").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/categories/**").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/fields/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**/fields/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/contact-log/add").authenticated()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/login/oauth2/code/**",
                                "/oauth/callback"
                        ).permitAll()
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

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(request, authentication);
            if (authorizedClient == null) {
                log.error("❌ Не удалось получить OAuth2AuthorizedClient");
                response.sendRedirect("http://localhost:4200/login?error=oauth_client_error");
                return;
            }

            // 🔹 Передаём пользователя и токены в `OAuth2AuthenticationService`
            String jwtToken = oAuth2AuthenticationService.processUserAuthentication(oidcUser, authorizedClient);

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
            log.error("❌ Ошибка: пользователь не является OIDC пользователем");
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

    /**
     * ✅ Получает OAuth2AuthorizedClient из контекста
     */
    private OAuth2AuthorizedClient getAuthorizedClient(HttpServletRequest request, Authentication authentication) {
        OAuth2AuthorizedClientService clientService = new InMemoryOAuth2AuthorizedClientService(new InMemoryClientRegistrationRepository());
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        return clientService.loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
    }

}