package kontactNick.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;  // ✅ Добавлено поле ID

    @NotBlank(message = "Name cannot be empty")
    private String name;
    private String description;
}
