package kontactNick.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kontactNick.entity.User;
import kontactNick.security.util.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public CustomOAuth2SuccessHandler(@Lazy UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // Пользователь успешно вошел через Google
            String email = oidcUser.getEmail();
            String nick = email;
            String avatarUrl = oidcUser.getPicture();

            System.out.println("OAuth2 Login Successful: " + email);

            // Сохраняем или обновляем пользователя в БД
            User user = userService.saveOrUpdateUser(email, nick, avatarUrl);

            // Генерируем JWT
            String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole());

            // Устанавливаем JSON-ответ
            response.setContentType("application/json");
            response.getWriter().write("{\"token\":\"" + jwt + "\"}");
            response.getWriter().flush();
        }
    }
}