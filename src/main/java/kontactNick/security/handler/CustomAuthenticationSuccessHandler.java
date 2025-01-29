package kontactNick.security.handler;

import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@Transactional
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, TokenService tokenService) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("✅ OAuth Login Success: {}", authentication.getName());

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            String fullName = oidcUser.getFullName();
            String avatarUrl = oidcUser.getPicture();

            // ⚠️ Если `fullName` пустой, используем email как `nick`
            String nick = (fullName != null && !fullName.isEmpty()) ? fullName : email;

            log.info("🔍 OAuth User Info: email={}, nick={}, avatarUrl={}", email, nick, avatarUrl);

            Optional<User> optionalUser = userRepository.findByEmail(email);

            User user = optionalUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setNick(nick);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setRole(Roles.ROLE_USER);
                log.info("🆕 New user registered: {}", email);
                return userRepository.save(newUser);
            });

            // ✅ Обновляем SecurityContext
            if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("🔐 SecurityContextHolder: Пользователь аутентифицирован -> {}", authentication.getName());
            } else {
                log.info("🔐 SecurityContextHolder уже содержит аутентификацию: {}", SecurityContextHolder.getContext().getAuthentication().getName());
            }

            // ✅ Генерация JWT токена
            String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
            log.info("🔑 Generated JWT: {}", jwt);

            if (jwt == null || jwt.isEmpty()) {
                log.error("❌ Ошибка: JWT не сгенерирован!");
            } else {
                // ✅ Устанавливаем JWT в Cookie
                boolean isSecure = request.isSecure();  // Проверяем, HTTPS или HTTP

                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", jwt)
                        .httpOnly(true)  // ✅ Защищаем Cookie от JavaScript
                        .secure(isSecure) // ✅ Динамическое определение (HTTPS → true, HTTP → false)
                        .path("/")
                        .maxAge(Duration.ofDays(1))
                        .sameSite(isSecure ? "None" : "Lax")  // ❗ Для кросс-доменных запросов нужен `None`
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                log.info("🍪 JWT сохранён в Cookie: {}", accessTokenCookie);
            }

            // ✅ Перенаправление пользователя после логина
            String redirectUrl = "http://localhost:4200/dashboard";  // ✅ Можно вынести в env
            log.info("➡ Перенаправляем пользователя на {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("❌ Ошибка аутентификации: не OIDC пользователь");
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }
}