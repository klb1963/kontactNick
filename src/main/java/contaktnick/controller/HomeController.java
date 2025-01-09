package contaktnick.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class HomeController {
    @GetMapping("/")
    public String home() {
        return "home"; // Вернёт страницу home.html из src/main/resources/templates
    }
}
