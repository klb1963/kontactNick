package kontactNick.controller;

import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto loginDto) {
        // Аутентификация пользователя
        String token = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        // Возвращаем токен в ответе
        return ResponseEntity.ok(token);
    }
}
