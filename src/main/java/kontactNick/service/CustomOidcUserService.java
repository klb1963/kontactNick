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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CustomOidcUserService extends OidcUserService {

    public CustomOidcUserService() {
        super();
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("🔄 Загружаем пользователя через CustomOidcUserService");

        // Получаем стандартного OIDC пользователя
        OidcUser oidcUser = super.loadUser(userRequest);

        // Достаём email пользователя
        String email = oidcUser.getEmail();
        log.info("🔍 Email пользователя: {}", email);

        // ✅ Теперь `CustomOidcUserService` НЕ отвечает за создание пользователя.
        // ✅ Это делает `OAuth2AuthenticationService`.

        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oidcUser.getIdToken(),
                "email"
        );
    }
}