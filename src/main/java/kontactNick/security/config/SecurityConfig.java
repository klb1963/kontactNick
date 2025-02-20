package kontactNick.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.repository.UserRepository;
import kontactNick.security.handler.CustomAuthenticationSuccessHandler;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.CustomOidcUserService;
import kontactNick.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Filter;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService customOidcUserService;

    @Lazy
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

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
                        .successHandler(customAuthenticationSuccessHandler)
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
    public OAuth2AuthorizedClientRepository authorizedClientRepository(ClientRegistrationRepository clientRegistrationRepository) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
        );
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