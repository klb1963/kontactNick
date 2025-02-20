package kontactNick.security.handler;

import kontactNick.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@Transactional
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("✅ Успешная аутентификация: {}", authentication.getName());

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            String fullName = oidcUser.getFullName();
            String avatarUrl = oidcUser.getPicture();
            String nick = (fullName != null && !fullName.isEmpty()) ? fullName : email;

            log.info("🔍 OAuth User Info: email={}, nick={}, avatarUrl={}", email, nick, avatarUrl);

            // 🔹 Перенаправляем пользователя
            response.sendRedirect("http://localhost:4200/dashboard");
        } else {
            log.error("❌ Ошибка: пользователь не является OIDC пользователем");
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }

}