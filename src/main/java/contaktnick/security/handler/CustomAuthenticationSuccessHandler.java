package contaktnick.security.handler;

import contaktnick.entity.Roles;
import contaktnick.entity.User;
import contaktnick.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Transactional
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String username = oidcUser.getFullName();

        // Проверка, существует ли пользователь в базе
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            // Создание нового пользователя
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(email);
            newUser.setRole(Roles.USER); // Установите роль по умолчанию

            System.out.println("Saving user: " + newUser);
            userRepository.save(newUser); // Сохраняем пользователя в базе
        }

        // Перенаправление после успешной аутентификации
        response.sendRedirect("/profile");
    }
}

//    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
//        String email = oidcUser.getEmail();
//        String username = oidcUser.getFullName();
//
//        // Проверка, существует ли пользователь в базе
//        Optional<User> optionalUser = userRepository.findByEmail(email);
//        if (optionalUser.isEmpty()) {
//            // Создание нового пользователя
//            User newUser = new User();
//            newUser.setEmail(email);
//            newUser.setNick(email); // Устанавливаем имя пользователя
//            newUser.setRole(Roles.USER);       // Устанавливаем роль
//            userRepository.save(newUser);
//        }
//
//        // Перенаправление после успешной аутентификации
//        response.sendRedirect("/profile");
//    }


