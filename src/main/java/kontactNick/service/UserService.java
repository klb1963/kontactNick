package kontactNick.service;

import lombok.extern.slf4j.Slf4j;
import kontactNick.dto.LoginDto;
import kontactNick.dto.UserDto;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setNick(user.getEmail());
        user.setRole(Roles.USER);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);
    }

    public String authenticate(String email, String password) {
    // Находим пользователя по email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Email not found: " + email);
                    return new IllegalArgumentException("Invalid email or password");
                });
        // Проверяем пароль
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("Password mismatch for user: " + email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Генерируем и возвращаем токен, включающий email и роль пользователя
        return jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        }

}
