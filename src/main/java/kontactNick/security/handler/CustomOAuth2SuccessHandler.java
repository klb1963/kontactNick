package kontactNick.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.entity.User;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
import kontactNick.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Autowired
    public CustomOAuth2SuccessHandler(@Lazy UserService userService, JwtTokenProvider jwtTokenProvider, TokenService tokenService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
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


            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }


            // ✅ Генерируем JWT
            String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole());

            // ✅ Логируем токен (удобно для отладки)
            System.out.println("🔑 Generated JWT: " + jwt);

            // ✅ Редиректим на фронт c токеном в URL
            String redirectUrl = "http://localhost:4200/dashboard";
            System.out.println("✅ Redirecting to: " + redirectUrl);
            final ResponseCookie accessTokenCookie = tokenService.generateCookie(jwt);
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.sendRedirect(redirectUrl);

        } else {
            System.out.println("❌ Authentication principal is not OidcUser");
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }
}

//  Совершаем редирект
//   response.sendRedirect("http://localhost:4200/dashboard?token=" + jwt);