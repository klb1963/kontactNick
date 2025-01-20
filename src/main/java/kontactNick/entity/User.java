package kontactNick.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kontactNick.entity.Category;
import kontactNick.entity.Roles;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "categories")  // ✅ Избегает рекурсии при логировании
@Entity
@Table(name = "users") // Явное указание имени таблицы
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String nick;

    @Column(nullable = false, unique = true) // ✅ Email должен быть уникальным
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING) // ✅ Enum хранится как строка
    @Column(nullable = false)
    private Roles role;

    @Column(nullable = true)
    private String avatarUrl; // ✅ Добавили поле для ссылки на аватар

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // ✅ Оптимизированная загрузка
    @JsonManagedReference
    private List<Category> categories = new ArrayList<>();
}


