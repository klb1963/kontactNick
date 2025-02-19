package kontactNick.security.handler;

import jakarta.servlet.http.Cookie;
import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import kontactNick.security.util.JwtTokenProvider;
import kontactNick.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Transactional
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("✅ [CustomAuthenticationSuccessHandler] Аутентификация успешна: {}", authentication.getName());

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            log.info("🔍 Обрабатываем OIDC пользователя");

            String email = oidcUser.getEmail();
            String fullName = oidcUser.getFullName();
            String avatarUrl = oidcUser.getPicture();
            String nick = (fullName != null && !fullName.isEmpty()) ? fullName : email;

            log.info("🔍 OAuth User Info: email={}, nick={}, avatarUrl={}", email, nick, avatarUrl);

            // 🔹 Проверяем, есть ли пользователь в базе
            Optional<User> optionalUser = userRepository.findByEmail(email);
            User user = optionalUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setNick(nick);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setRole(Roles.ROLE_USER);
                log.info("🆕 Новый пользователь зарегистрирован: {}", email);
                return userRepository.save(newUser);
            });

            // 🔹 Получаем `OAuth2AuthorizedClient`
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("google", authentication.getName());

            // ✅ Добавляем проверку и логирование
            if (authorizedClient == null) {
                log.warn("⚠️ OAuth2AuthorizedClient не найден, возможно, пользователь только что зарегистрировался.");
            }

            OAuth2AccessToken accessToken = authorizedClient != null ? authorizedClient.getAccessToken() : null;
            OAuth2RefreshToken refreshToken = authorizedClient != null ? authorizedClient.getRefreshToken() : null;

            if (accessToken != null) {
                Instant tokenExpiry = accessToken.getExpiresAt();
                long expiresIn = Duration.between(Instant.now(), tokenExpiry).getSeconds();

                user.setGoogleAccessToken(accessToken.getTokenValue());
                user.setGoogleTokenExpiry(tokenExpiry);
                log.info("✅ Google Access Token сохранен, истекает через {} секунд", expiresIn);
            } else {
                log.error("❌ Ошибка: Google Access Token отсутствует!");
            }

            if (refreshToken != null) {
                user.setGoogleRefreshToken(refreshToken.getTokenValue());
                log.info("✅ Google Refresh Token сохранен.");
            } else {
                log.warn("⚠️ У Google отсутствует `refresh_token`, возможно, не был запрошен `access_type=offline`.");
            }

            userRepository.save(user);

            // 🔹 Генерация JWT токена
            String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
            log.info("🔑 Generated JWT: {}", jwt);

            if (jwt == null || jwt.isEmpty()) {
                log.error("❌ Ошибка: JWT не сгенерирован!");
            } else {
                boolean isSecure = request.isSecure();
                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt-token", jwt)
                        .httpOnly(true)
                        .secure(isSecure)
                        .path("/")
                        .maxAge(Duration.ofDays(1))
                        .sameSite(isSecure ? "None" : "Lax")
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                log.info("🍪 JWT сохранён в Cookie: {}", accessTokenCookie);
            }

            // 🔹 Перенаправление пользователя
            String redirectUrl = "http://localhost:4200/dashboard";
            log.info("➡ Перенаправляем пользователя на {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("❌ Ошибка: пользователь не является OIDC пользователем");
            response.sendRedirect("http://localhost:4200/login?error=authentication_failed");
        }
    }
}