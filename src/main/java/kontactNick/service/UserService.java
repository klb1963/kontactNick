package kontactNick.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import kontactNick.dto.UserDto;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserService {

    // DI
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 👨register new user
    public void register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setNick(userDto.getEmail());
        user.setRole(Roles.ROLE_USER);  // ✅ Устанавливаем корректную роль
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);
        log.info("✅ User registered successfully: {}", user.getEmail());
    }

    // ✅ user authentication
    public String authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Authentication failed: Email not found: {}", email);
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("❌ Authentication failed: Password mismatch for user: {}", email);
            throw new BadCredentialsException("Invalid email or password");  // ✅ Исправлено
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        log.info("✅ User authenticated successfully: {}", email);
        return token;
    }

    // 💾save or update user
    @Transactional
    public User saveOrUpdateUser(String email, String nick, String avatarUrl) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setNick(nick);
            user.setAvatarUrl(avatarUrl);

            // ✅ Убеждаемся, что роль корректная
            if (user.getRole() == null) {
                user.setRole(Roles.ROLE_USER);
            }

            return userRepository.save(user);
        } else {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(nick);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole(Roles.ROLE_USER);

            return userRepository.save(newUser);
        }
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}