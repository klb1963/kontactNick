package kontactNick.service;

import kontactNick.entity.Roles;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("🔄 Загружаем пользователя через CustomOidcUserService");

        // Получаем стандартного OIDC пользователя
        OidcUser oidcUser = super.loadUser(userRequest);

        // Достаём email
        String email = oidcUser.getEmail();
        log.info("🔍 Email пользователя: {}", email);

        // Проверяем, есть ли пользователь в базе
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user = optionalUser.orElseGet(() -> {
            log.info("🆕 Новый пользователь: {}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNick(oidcUser.getFullName() != null ? oidcUser.getFullName() : email);
            newUser.setAvatarUrl(oidcUser.getPicture());
            newUser.setRole(Roles.ROLE_USER);
            return userRepository.save(newUser);
        });

        // Возвращаем OIDC пользователя с обновлёнными атрибутами
        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                oidcUser.getIdToken(),
                "email"
        );
    }

}
