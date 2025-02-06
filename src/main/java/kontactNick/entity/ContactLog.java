package kontactNick.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data // ✅ Lombok: добавляет геттеры, сеттеры, toString(), equals(), hashCode()
@Table(name = "contact_log")
public class ContactLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("currentUserNick") // 👈 Соответствует ключу из JSON
    @Column(name = "user_nick", nullable = false)
    private String userNick; // Ник пользователя, который добавил контакт

    @JsonProperty("addedUserNick") // 👈 Соответствует ключу из JSON
    @Column(name = "contact_nick", nullable = false)
    private String contactNick; // Ник нового контакта

    @Column(name = "category", nullable = false)
    private String category; // ✅ Добавляем категорию

    @ElementCollection
    @CollectionTable(name = "contact_log_fields", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "field_name")
    private List<String> fields; // ✅ Список полей контакта

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ✅ Автоматически устанавливаем `createdAt` перед сохранением в БД
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}