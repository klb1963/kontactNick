package kontactNick.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kontactNick.dto.GoogleUser;
import lombok.extern.slf4j.Slf4j;
import kontactNick.dto.UserDto;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * ✅ Регистрирует нового пользователя
     */
    public void register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email уже используется");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setNick(userDto.getEmail());
        user.setRole(Roles.ROLE_USER);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);
        log.info("✅ Пользователь зарегистрирован: {}", user.getEmail());
    }

    /**
     * ✅ Аутентификация пользователя
     */
    public String authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Ошибка аутентификации: Email не найден: {}", email);
                    return new BadCredentialsException("Неверный email или пароль");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("❌ Ошибка аутентификации: Неверный пароль для {}", email);
            throw new BadCredentialsException("Неверный email или пароль");
        }

        log.info("🚀 Генерация JWT для пользователя: {}", email);
        return jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
    }

    /**
     * ✅ Получает или создаёт пользователя
     */
    @Transactional
    public User getOrCreateUser(String email, String nick, String avatarUrl) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(nick);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole(Roles.ROLE_USER);
            log.info("🆕 Новый пользователь зарегистрирован: {}", email);
            return userRepository.save(newUser);
        });
    }

    /**
     * ✅ Получает текущий ник пользователя
     */
    public String getCurrentUserNick() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String nick = oauth2User.getAttribute("nickname");
            if (nick == null) {
                nick = oauth2User.getAttribute("name");
            }
            if (nick == null) {
                nick = oauth2User.getAttribute("preferred_username");
            }
            return nick != null ? nick : "Unknown";
        }
        return "Unknown";
    }

    /**
     * ✅ Получает пользователя по его email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}