package kontactNick.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "fields")  // ✅ Избегает рекурсии при логировании
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // имя категории

    @Column(nullable = true)
    private String description; // описание категории

    @ManyToOne(fetch = FetchType.LAZY)  // ✅ Оптимизированная загрузка пользователя
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user; // владелец категории

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)  // ✅ Оптимизированная загрузка полей
    @JsonManagedReference
    private List<Field> fields = new ArrayList<>();
}