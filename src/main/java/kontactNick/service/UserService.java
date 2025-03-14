package kontactNick.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kontactNick.dto.GoogleUser;
import kontactNick.exception_handling.exceptions.DuplicateEmailException;
import kontactNick.exception_handling.exceptions.NickAlreadyTakenException;
import kontactNick.exception_handling.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
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

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ✅ Регистрирует нового пользователя
     */
    public void register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new DuplicateEmailException("Email уже используется");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setNick(userDto.getNick() != null ? userDto.getNick() : userDto.getEmail());
        user.setRole(Roles.ROLE_USER);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        userRepository.save(user);
        log.info("✅ Пользователь зарегистрирован: {}", user.getEmail());
    }

    /**
     * ✅ Аутентификация пользователя и генерация JWT
     */
    public String authenticate(String email, String password) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("❌ Ошибка аутентификации: Неверный пароль для {}", email);
            throw new BadCredentialsException("Неверный email или пароль");
        }

        log.info("🚀 Генерация JWT для пользователя: {}", email);
        return jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
    }

    /**
     * ✅ Получение пользователя по email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + email));
    }

    /**
     * ✅ Проверка доступности никнейма
     */
    public boolean isNickAvailable(String nick) {
        return userRepository.findByNick(nick).isEmpty();
    }

    /**
     * ✅ Обновление никнейма пользователя
     */
    public void updateNick(String email, String newNick) {
        if (newNick == null || newNick.isBlank()) {
            throw new IllegalArgumentException("Nick cannot be empty");
        }

        if (!isNickAvailable(newNick)) {
            throw new NickAlreadyTakenException("Nick уже занят");
        }

        User user = getUserByEmail(email);
        user.setNick(newNick);
        userRepository.save(user);
        log.info("✅ Nick пользователя '{}' обновлен на '{}'", user.getEmail(), newNick);
    }

    /**
     * ✅ Получает текущий ник пользователя (для OAuth2)
     */
    public String getCurrentUserNick() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("nickname") != null ? oauth2User.getAttribute("nickname") :
                    oauth2User.getAttribute("name") != null ? oauth2User.getAttribute("name") :
                            oauth2User.getAttribute("preferred_username") != null ? oauth2User.getAttribute("preferred_username") :
                                    "Unknown";
        }
        return "Unknown";
    }
}