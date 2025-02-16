package kontactNick.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.servlet.http.HttpServletResponse;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/oauth2/callback")
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final GoogleTokenService googleTokenService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/google")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam("code") String authorizationCode, HttpServletResponse response) {
        log.info("📥 Получен OAuth2 код авторизации от Google: {}", authorizationCode);

        // 1️⃣ Обмен кода на токены
        Optional<Map<String, String>> tokensOptional = googleTokenService.exchangeAuthorizationCodeForTokens(authorizationCode);

        if (tokensOptional.isEmpty() || !tokensOptional.get().containsKey("id_token")) {
            log.error("❌ Ошибка обмена кода на токены!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка авторизации через Google");
        }

        Map<String, String> tokens = tokensOptional.get();

        String idToken = tokens.get("id_token");
        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.getOrDefault("refresh_token", "");

        log.info("✅ Google Access Token: {}", accessToken);
        log.info("✅ Google ID Token: {}", idToken);

        // 2️⃣ Декодируем id_token, чтобы получить информацию о пользователе
        GoogleIdToken.Payload payload = decodeGoogleIdToken(idToken);
        if (payload == null) {
            log.error("❌ Ошибка декодирования id_token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка декодирования id_token");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // 3️⃣ Проверяем, есть ли пользователь в базе
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(name);
            newUser.setAvatarUrl(picture);
            newUser.setRole(Roles.ROLE_USER);
            log.info("🆕 Новый пользователь зарегистрирован: {}", email);
            return userRepository.save(newUser);
        });

        // 4️⃣ Обновляем токены в БД
        user.setGoogleAccessToken(accessToken);
        user.setGoogleRefreshToken(refreshToken);
        user.setGoogleTokenExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        // 5️⃣ Генерируем JWT токен
        String jwtToken = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        // 6️⃣ Устанавливаем JWT в куки
        ResponseCookie cookie = ResponseCookie.from("jwt-token", jwtToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("🍪 Установлен JWT-токен для пользователя: {}", email);

        // 7️⃣ Перенаправляем пользователя на фронтенд
        return ResponseEntity.ok(Map.of("redirect", "http://localhost:4200/dashboard"));
    }

    private GoogleIdToken.Payload decodeGoogleIdToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                    .setAudience(Collections.singletonList(System.getenv("GOOGLE_CLIENT_ID")))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            return googleIdToken != null ? googleIdToken.getPayload() : null;
        } catch (Exception e) {
            log.error("Ошибка при валидации ID токена: {}", e.getMessage());
            return null;
        }
    }

}
