package kontactNick.security.config;

import kontactNick.security.handler.CustomAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll() // Разрешить доступ к Swagger UI
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN") // USER или ADMIN
                        .anyRequest().authenticated() // Остальные запросы требуют авторизации
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/api/profile", true) // Перенаправление после успешной авторизации
                        .failureUrl("/login?error") // В случае ошибки
                        .successHandler(customAuthenticationSuccessHandler) // Обработчик успешной авторизации
                );

        return http.build();
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }
}

//                .formLogin() // Включить стандартную форму входа
//                .permitAll(); // Разрешить доступ ко всем ресурсам формы входа
// Доступ открыт всем
// .csrf().disable() // Отключаем CSRF (не рекомендуется в продакшене)
//                .authorizeHttpRequests(authorize -> authorize
//        .anyRequest().permitAll() // Разрешить доступ ко всем путям
//                );