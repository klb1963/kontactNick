package kontactNick.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // Пользователь успешно вошел через Google
            System.out.println("OAuth2 Login Successful: " + oidcUser.getEmail());

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