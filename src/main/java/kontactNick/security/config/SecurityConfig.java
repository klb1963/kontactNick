package kontactNick.security.config;

import jakarta.servlet.http.HttpServletResponse;
import kontactNick.security.handler.CustomOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // Разрешаем CORS
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/auth/register", "/api/auth/login", "/api/auth/token").permitAll() // ✅ Открытые эндпоинты
                        .requestMatchers("/api/oauth2/profile").authenticated() // ✅ Требует JWT
                        .requestMatchers("/api/categories").hasAuthority("ROLE_USER") // ✅ Требует роль
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ Отключаем сессии
                .securityContext(securityContext -> securityContext.securityContextRepository(new HttpSessionSecurityContextRepository()))
//                .securityContext(securityContext -> securityContext.disable()) // ✅ Отключаем SecurityContext
//                .sessionManagement(session -> session.sessionFixation().none()) // ✅ Отключаем смену sessionId
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2SuccessHandler) // Обрабатываем успешный вход
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/").permitAll() // Разрешаем выход из системы
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // ✅ JWT перед аутентификацией
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200")); // ✅ Разреши фронт (или нужный домен)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
