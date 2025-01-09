package contaktnick.controller;

import contaktnick.entity.User;
import contaktnick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public String createUser(@RequestBody User user) {
        // Здесь сохраняем пользователя в базе данных
        return "User " + user.getNick() + " created!";
    }

    @GetMapping("/{nick}")
    public ResponseEntity<User> getUserByNick(@PathVariable String email) {
        return ResponseEntity.of(userRepository.findByEmail(email));
    }

}
