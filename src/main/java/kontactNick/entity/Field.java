package kontactNick.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;

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
    @JsonBackReference
    private Category category; // Категория, к которой относится поле
}
