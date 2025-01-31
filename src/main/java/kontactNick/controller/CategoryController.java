package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.dto.FieldDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import kontactNick.service.CategoryService;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api/categories") // ✅ API теперь корректно структурировано
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequiredArgsConstructor
public class CategoryController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FieldRepository fieldRepository;

    // ✅ Создание категории
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryDto categoryDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.debug("Authenticated email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.debug("Found user: {} with ID: {}", user.getEmail(), user.getId());

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setUser(user);

        Category savedCategory = categoryRepository.save(category);
        log.debug("Saved category '{}' for user ID: {}", savedCategory.getName(), savedCategory.getUser().getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // ✅ Получение всех категорий пользователя
    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<Category> categories = categoryRepository.findByUser_Email(email);

        return ResponseEntity.ok(categories);
    }

    // ✅ Добавление одного поля в категорию
    @PostMapping("/{categoryId}/field")
    public ResponseEntity<String> addFieldToCategory(@PathVariable Long categoryId, @RequestBody FieldDto fieldRequest) {
        log.debug("Received request to add field to category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        log.debug("Category found: {}", category.getName());

        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setValue(fieldRequest.getValue());
        field.setCategory(category);

        fieldRepository.save(field);
        log.debug("Field saved successfully with ID: {}", field.getId());

        return ResponseEntity.ok("Field added to category successfully");
    }

    // ✅ Добавление нескольких полей в категорию (Используем `FieldDto`)
    @PostMapping("/{categoryId}/fields")
    public ResponseEntity<String> addFieldsToCategory(@PathVariable Long categoryId, @RequestBody List<FieldDto> fieldRequests) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<Field> fields = fieldRequests.stream().map(fieldRequest -> {
            Field field = new Field();
            field.setName(fieldRequest.getName());
            field.setFieldType(fieldRequest.getFieldType());
            field.setValue(fieldRequest.getValue());
            field.setCategory(category);
            return field;
        }).collect(Collectors.toList());

        fieldRepository.saveAll(fields);

        return ResponseEntity.ok("Fields added to category successfully");
    }

    // ✅ Получение полей в категории
    @GetMapping("/{categoryId}/fields")
    public ResponseEntity<List<FieldDto>> getFieldsByCategory(@PathVariable Long categoryId) {
        log.debug("Fetching fields for category ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<FieldDto> fields = category.getFields().stream()
                .map(field -> new FieldDto(
                        field.getId(),
                        field.getName(),
                        field.getDescription(),
                        field.getFieldType(),
                        field.getValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(fields);
    }

    // ✅ Обновление категории
    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long categoryId, @RequestBody CategoryDto categoryDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(categoryDto.getName());
        categoryRepository.save(category);

        return ResponseEntity.ok(category);
    }

    // ✅ Удаление категории
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

}