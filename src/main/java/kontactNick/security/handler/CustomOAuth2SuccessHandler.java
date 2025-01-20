package kontactNick.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

    @Autowired
    public CustomOAuth2SuccessHandler(@Lazy UserService userService) { // ✅ Инжектируем UserService, @Lazy разрывает цикл
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // Пользователь успешно вошел через Google
            String email = oidcUser.getEmail();
            String nick = email;
            String avatarUrl = oidcUser.getPicture();

            System.out.println("OAuth2 Login Successful: " + email);
            System.out.println("Nick before saving: " + nick);
            System.out.println("Avatar URL: " + avatarUrl);

            // Сохраняем или обновляем пользователя в БД
            userService.saveOrUpdateUser(email, nick, avatarUrl);

            // Сохраняем пользователя в SecurityContext
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Явно сохраняем контекст в сессии
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            // Перенаправляем на профиль
            response.sendRedirect("/api/oauth2/profile");
            return;
        }

        // Если не OIDC-пользователь, выполняем стандартное поведение
        super.onAuthenticationSuccess(request, response, authentication);
    }
}