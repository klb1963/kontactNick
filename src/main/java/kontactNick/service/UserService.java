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

    // DI
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;  // ✅ Добавляем RestTemplate
    private final UserGoogleTokenService userGoogleTokenService;


    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, RestTemplate restTemplate, UserGoogleTokenService userGoogleTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplate;
        this.userGoogleTokenService = userGoogleTokenService;
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

        log.info("🚀 Generating new JWT in authenticate() for user: {}", email);
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

    public String getCurrentUserNick() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttribute("email"); // ✅ Можно заменить на `nickname`
        }
        return "Unknown";
    }

    public GoogleUser getGoogleUserInfo(String accessToken) {
        log.info("🔍 Fetching Google user info...");

        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                GoogleUser user = new GoogleUser();
                user.setEmail(jsonNode.get("email").asText());
                user.setNick(jsonNode.get("name").asText());
                user.setPicture(jsonNode.get("picture").asText());
                return user;
            } catch (Exception e) {
                log.error("❌ Error parsing Google user info response", e);
            }
        }
        log.error("❌ Failed to fetch Google user info, response: {}", response.getBody());
        return null;
    }

    @Transactional
    public User registerOrUpdateGoogleUser(GoogleUser googleUser) {
        log.info("🔄 Registering/updating Google user: {}", googleUser.getEmail());

        User user = userRepository.findByEmail(googleUser.getEmail()).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(googleUser.getEmail());
            newUser.setNick(googleUser.getNick()); // ✅ Используем `nick` вместо `name`
            newUser.setAvatarUrl(googleUser.getPicture());
            newUser.setRole(Roles.ROLE_USER); // ✅ По умолчанию даём обычную роль
            return newUser;
        });

        // ✅ Обновляем данные (на случай, если у пользователя обновился email/аватар)
        user.setNick(googleUser.getNick());
        user.setAvatarUrl(googleUser.getPicture());

        return userRepository.save(user);
    }

    public String getUserAccessToken(User user) {
        return userGoogleTokenService.getValidAccessToken(user);
    }

}