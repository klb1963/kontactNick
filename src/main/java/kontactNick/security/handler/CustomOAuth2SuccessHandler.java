package kontactNick.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public CustomOAuth2SuccessHandler(@Lazy UserService userService, JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        System.out.println("🔄 CustomOAuth2SuccessHandler triggered!");

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // ✅ Получаем данные пользователя из Google OAuth
            String email = oidcUser.getEmail();
            String nick = email;
            String avatarUrl = oidcUser.getPicture();

            System.out.println("✅ OAuth2 Login Successful: " + email);

            // ✅ Сохраняем/обновляем пользователя в БД
            User user = userService.saveOrUpdateUser(email, nick, avatarUrl);

            // ✅ Генерируем JWT
            String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole());

            // ✅ Логируем токен (удобно для отладки)
            System.out.println("🔑 Generated JWT: " + jwt);

            // ✅ Сохраняем токен в сессии
            HttpSession session = request.getSession();
            session.setAttribute("jwt", jwt);
            System.out.println("✅ JWT stored in session: " + session.getAttribute("jwt"));

            // ✅ Редиректим на фронт c токеном в URL
            String redirectUrl = "http://localhost:4200/dashboard";
            System.out.println("✅ Redirecting to: " + redirectUrl);
            String token = objectMapper.writeValueAsString(Collections.singletonMap("token", jwt));
            response.getWriter().write(token);
            response.sendRedirect(redirectUrl);

        } else {
            System.out.println("❌ Authentication principal is not OidcUser");
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }
}

//  Совершаем редирект
//   response.sendRedirect("http://localhost:4200/dashboard?token=" + jwt);