package com.contaktnick.contaktnick.controller;

import com.contaktnick.contaktnick.entity.User;
import com.contaktnick.contaktnick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userRepository.save(user));
    }
    @GetMapping("/{nick}")
    public ResponseEntity<User> getUserByNick(@PathVariable String nick) {
        return ResponseEntity.of(userRepository.findByNick(nick));
    }

}
