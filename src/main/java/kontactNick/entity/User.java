package kontactNick.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kontactNick.entity.Category;
import kontactNick.entity.Roles;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "categories")  // ✅ Избегает рекурсии при логировании
@Entity
@Table(name = "users") // Явное указание имени таблицы
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String nick;

    @Column(nullable = true) // ✅ Добавляем поле "name" для хранения полного имени пользователя
    private String name;

    @Column(nullable = false, unique = true) // ✅ Email должен быть уникальным
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING) // ✅ Enum хранится как строка
    @Column(nullable = false)
    private Roles role;

    @Column(nullable = true)
    private String avatarUrl; // ✅ Это соответствует "picture" из Google OAuth

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // ✅ Оптимизированная загрузка
    @JsonManagedReference
    private List<Category> categories = new ArrayList<>();

    @Column(length = 2048, nullable = true)
    private String googleAccessToken;

    @Column(length = 2048, nullable = true)
    private String googleRefreshToken;

    @Column(nullable = true)
    private Instant googleTokenExpiry;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}