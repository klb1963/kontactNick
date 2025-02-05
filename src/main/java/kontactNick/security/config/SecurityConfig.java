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
import org.springframework.web.cors.CorsUtils;
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
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll() // ✅ Разрешение pre-flight запросов
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()     // ✅ Разрешаем все OPTIONS-запросы
                        .requestMatchers("/", "/home").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()                // ✅ Разрешаем доступ ко всем auth маршрутам
                        .requestMatchers("/api/auth/external-login").permitAll()    // ✅ Разрешаем доступ к внешнему логину
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/profile").authenticated()            // ✅ Доступ к профилю
                        .requestMatchers("/api/categories/**").hasAuthority("ROLE_USER")  // ✅ Доступ к категориям
                        .requestMatchers(HttpMethod.PUT, "/api/fields/**").authenticated() // ✅ Обновление полей
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**/fields/**").authenticated() // ✅ Удаление полей
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2.successHandler(customAuthenticationSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("❌ Unauthorized request to {}", request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200"); // ✅ Поддержка CORS
                            response.setHeader("Access-Control-Allow-Credentials", "true");             // ✅ Поддержка credentials
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // ✅ Добавлены OPTIONS
        configuration.setAllowCredentials(true); // ✅ Разрешение для передачи куков
        configuration.setAllowedHeaders(List.of("*")); // ✅ Разрешение всех заголовков

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}



//@Bean
//public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    http
//            .cors(withDefaults())
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(auth -> auth
//                    .requestMatchers(CorsUtils::isPreFlightRequest).permitAll() // ✅ Разрешение pre-flight запросов
//                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // ✅ Разрешаем все OPTIONS-запросы
//                    .requestMatchers("/", "/home").permitAll()
//                    .requestMatchers("/api/auth/**").permitAll()
//                    .requestMatchers("/api/public/**").permitAll()
//                    .requestMatchers("/api/oauth2/profile").authenticated()
//                    .requestMatchers("/api/categories/**").hasAuthority("ROLE_USER")
//                    .requestMatchers(HttpMethod.PUT, "/api/fields/**").authenticated()
//                    .requestMatchers(HttpMethod.DELETE, "/api/categories/**/fields/**").authenticated()
//                    .requestMatchers("/api/auth/token").authenticated()
//                    .anyRequest().authenticated()
//            )
//            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//            .oauth2Login(oauth2 -> oauth2.successHandler(customAuthenticationSuccessHandler))
//            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
//            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//            .exceptionHandling(exc -> exc
//                    .authenticationEntryPoint((request, response, authException) -> {
//                        log.warn("❌ Unauthorized request to {}", request.getRequestURI());
//                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                        response.setContentType("application/json");
//                        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200"); // ✅ Добавление заголовка
//                        response.setHeader("Access-Control-Allow-Credentials", "true");             // ✅ Поддержка credentials
//                        response.getWriter().write("{\"error\": \"Unauthorized\"}");
//                    })
//            );
//
//    return http.build();
//}