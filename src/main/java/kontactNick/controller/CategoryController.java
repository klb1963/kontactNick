package kontactNick.controller;

import jakarta.validation.Valid;
import kontactNick.dto.CategoryDto;
import kontactNick.dto.FieldDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import kontactNick.service.CategoryService;
import kontactNick.service.FieldService;
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequiredArgsConstructor
public class CategoryController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FieldRepository fieldRepository;
    private final FieldService fieldService;

    // ✅ Создание категории (Только локально, без Google)
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🔑 Authenticated user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User {} not found!", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setUser(user);

        // 🔥 Сохраняем только в локальной базе (без Google)
        Category savedCategory = categoryRepository.save(category);
        log.info("✅ Created local category '{}' for user '{}'", savedCategory.getName(), email);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // ✅ Получение одной категории
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    // ✅ Получение всех локальных категорий пользователя
    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching local categories for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User {} not found!", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        List<Category> categories = categoryRepository.findByUser_Email(email);
        log.info("📂 Found {} local categories for user {}", categories.size(), email);

        return ResponseEntity.ok(categories);
    }

    // ✅ Обновление категории (Только локально, без Google)
    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long categoryId, @RequestBody CategoryDto categoryDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(email))
                .orElseThrow(() -> {
                    log.warn("❌ Attempt to update category {} failed, user {} has no access!", categoryId, email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        categoryRepository.save(category);

        log.info("✅ Updated local category '{}' for user '{}'", category.getName(), email);
        return ResponseEntity.ok(category);
    }

    // ✅ Удаление категории (Только локально, без Google)
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(email))
                .orElseThrow(() -> {
                    log.warn("❌ Attempt to delete category {} failed, user {} has no access!", categoryId, email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        categoryRepository.delete(category);
        log.info("🗑 Deleted local category '{}' for user '{}'", category.getName(), email);
        return ResponseEntity.noContent().build();
    }

    // ✅ Получение полей категории (Локально)
    @GetMapping("/{categoryId}/fields")
    public ResponseEntity<List<FieldDto>> getFieldsByCategory(@PathVariable Long categoryId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching fields for local category ID: {} by user: {}", categoryId, email);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.getUser().getEmail().equals(email)) {
            log.warn("❌ Access denied. Category '{}' does not belong to user '{}'", category.getName(), email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<FieldDto> fields = category.getFields().stream()
                .map(field -> new FieldDto(
                        field.getId(),
                        field.getName(),
                        field.getDescription(),
                        field.getFieldType(),
                        field.getValue()))
                .collect(Collectors.toList());

        log.info("✅ Found {} fields for category '{}' (ID: {})", fields.size(), category.getName(), categoryId);
        return ResponseEntity.ok(fields);
    }

    // ✅ Добавление поля в категорию (Локально)
    @PostMapping("/{categoryId}/fields")
    public ResponseEntity<Field> addFieldToCategory(@PathVariable Long categoryId, @RequestBody FieldDto fieldRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Adding field to local category ID: {} by user: {}", categoryId, email);

        Field savedField = fieldService.addFieldToCategory(categoryId, fieldRequest, email);
        log.info("✅ Field '{}' added to category '{}'", savedField.getName(), savedField.getCategory().getName());

        return ResponseEntity.ok(savedField);
    }

    // ✅ Обновление поля в категории (Локально)
    @PutMapping("/{categoryId}/fields/{fieldId}")
    public ResponseEntity<FieldDto> updateField(
            @PathVariable Long categoryId,
            @PathVariable Long fieldId,
            @RequestBody FieldDto updatedField) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Field field = fieldRepository.findById(fieldId)
                .filter(f -> f.getCategory().getId().equals(categoryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));

        if (!category.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        field.setName(updatedField.getName());
        field.setFieldType(updatedField.getFieldType());
        field.setValue(updatedField.getValue());

        fieldRepository.save(field);
        return ResponseEntity.ok(new FieldDto(field.getId(), field.getName(), field.getDescription(), field.getFieldType(), field.getValue()));
    }

    // ✅ Удаление поля из категории (Локально)
    @DeleteMapping("/{categoryId}/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long categoryId, @PathVariable Long fieldId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🗑 Deleting field ID: {} from local category ID: {} for user: {}", fieldId, categoryId, email);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Field field = fieldRepository.findById(fieldId)
                .filter(f -> f.getCategory().getId().equals(categoryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found or doesn't belong to category"));

        if (!category.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        fieldRepository.delete(field);
        return ResponseEntity.noContent().build();
    }
}