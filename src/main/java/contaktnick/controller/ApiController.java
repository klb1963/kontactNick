package contaktnick.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }
}
