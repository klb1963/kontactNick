package kontactNick.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

            // Логируем токен
            System.out.println("Generated JWT: " + jwt);

            // ✅ Редиректим с токеном в URL
            response.sendRedirect("http://localhost:4200/dashboard?token=" + jwt);
        }
    }
}


// Устанавливаем JSON-ответ
//            response.setContentType("application/json");
//            response.getWriter().write("{\"token\":\"" + jwt + "\"}");
//            response.getWriter().flush();

// Делаем редирект на фронтенд, передавая токен в URL
// System.out.println("Redirecting to frontend with token: " + jwt);
// response.sendRedirect("http://localhost:4200/dashboard?token=" + jwt);

// ✅ Сохраняем токен в HTTP-only Cookie (защищает от XSS)
//Cookie jwtCookie = new Cookie("jwt", jwt);
//            jwtCookie.setHttpOnly(true); // Делаем его недоступным для JS
//            jwtCookie.setPath("/"); // Доступен для всего приложения
//            jwtCookie.setMaxAge(60 * 60 * 24); // 1 день жизни
//
//            response.addCookie(jwtCookie);