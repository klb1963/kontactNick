package contaktnick.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class LoginController {

    @GetMapping("/")
    public String home() {
        return "home"; // Отобразите главную страницу
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Отобразите страницу входа
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        model.addAttribute("user", oidcUser);
        return "profile"; // Отобразите профиль пользователя
    }
}
