package kontactNick.security.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import kontactNick.security.handler.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors().and() // Разрешаем CORS
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/auth/register", "/api/auth/login").permitAll() // Разрешаем доступ на логин и регистрацию
                        .requestMatchers("/api/oauth2/profile").authenticated()
                        .requestMatchers("/api/categories").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.POST, "/api/categories/{categoryId}/field").hasAuthority("ROLE_USER") // Доступ к полям
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // Включаем сессию для OAuth2
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new CustomOAuth2SuccessHandler()) // Обрабатываем успешный вход
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/").permitAll() // Разрешаем выход из системы
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Добавляем JWT фильтр
                .addFilterAfter((request, response, chain) -> {
                    if (request instanceof HttpServletRequest) {
                        HttpServletRequest httpRequest = (HttpServletRequest) request;
                        System.out.println("Request URL: " + httpRequest.getRequestURL());
                    }
                    chain.doFilter(request, response);
                }, SecurityContextHolderFilter.class);

        return http.build();
    }

}