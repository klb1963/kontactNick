package kontactNick.entity;

import jakarta.persistence.*;
import kontactNick.entity.Category;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "category") // ✅ Избегает рекурсии при логировании
@Entity
@Table(name = "fields") // ✅ Явное имя таблицы
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Название поля

    @Column(nullable = true)
    private String description; // Описание поля (может быть пустым)

    @Column(nullable = true)
    private String value; // Значение поля

    @Column(name = "field_type", nullable = false)
    private String fieldType; // Тип поля (например, String, Number)

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Оптимизированная загрузка
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // Категория, к которой относится поле
}
