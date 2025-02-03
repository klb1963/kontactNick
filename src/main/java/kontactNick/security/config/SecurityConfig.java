package kontactNick.security.config;

import jakarta.servlet.http.HttpServletResponse;
import kontactNick.security.handler.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // ✅ Разрешаем CORS
                .csrf(csrf -> csrf.disable()) // ✅ Отключаем CSRF (не нужен для API)
                .authorizeHttpRequests(auth -> auth
                        // ✅ Открытые эндпоинты
                        .requestMatchers("/", "/home").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // ✅ Открываем только эндпоинты аутентификации
                        .requestMatchers("/api/public/**").permitAll() // ✅ Добавляем зону публичных API
                        .requestMatchers("/api/oauth2/profile").authenticated() // ✅ Требует аутентификации
                        .requestMatchers("/api/categories/**").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/fields/**").authenticated() // ✅ разрешение на обновление полей
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**/fields/**").authenticated() // удаление полей
                        .requestMatchers("/api/auth/token").authenticated() // ✅ Требует аутентификации
                        .anyRequest().authenticated() // ✅ Все остальные запросы требуют авторизации
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ Stateless аутентификация (JWT)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customAuthenticationSuccessHandler) // ✅ Используем кастомный successHandler
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/").permitAll() // ✅ Разрешаем выход
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // ✅ Добавляем JWT фильтр перед стандартной аутентификацией
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("❌ Unauthorized request to {}", request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setHeader("WWW-Authenticate", "Bearer realm=\"Access to API\""); // 🔥 Добавлен заголовок
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // ✅ Расширен список методов
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}