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
import kontactNick.service.GoogleContactsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@RestController
@RequestMapping("/api/contact-groups")
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequiredArgsConstructor
public class CategoryGroupController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FieldRepository fieldRepository;
    private final FieldService fieldService;
    private final CategoryService categoryService;
    private final GoogleContactsService googleContactsService;
    private static final Logger logger = LoggerFactory.getLogger(CategoryGroupController.class);

    // ✅ Создание категории (синхронизация с Google Contacts)
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🔑 Authenticated user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setUser(user);

        // 📡 Сохраняем в БД и синхронизируем с Google
        Category savedCategory = categoryService.createCategoryWithGoogleSync(category, user);
        log.info("✅ Created category '{}' for user '{}'", savedCategory.getName(), email);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // ✅ Получение всех категорий пользователя
    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching categories for user: {}", email);

        List<Category> categories = categoryRepository.findByUser_Email(email);
        log.info("📂 Found {} categories for user {}", categories.size(), email);

        return ResponseEntity.ok(categories);
    }

    // ✅ Получение одной категории
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(id)
                .filter(cat -> cat.getUser().getEmail().equals(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied"));

        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    // ✅ Обновление категории (синхронизация с Google)
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDto categoryDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        log.debug("🔄 Updating category ID: {} for user: {}", id, email);

        Category updatedCategory = categoryService.updateCategoryWithGoogleSync(id, categoryDto.getName(), user);
        return ResponseEntity.ok(updatedCategory);
    }

    // ✅ Удаление категории (удаление из Google Contacts)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        log.debug("🗑 Deleting category ID: {} for user: {}", id, email);

        categoryService.deleteCategoryWithGoogleSync(id, user);
        return ResponseEntity.noContent().build();
    }

    // ✅ Получение полей категории
    @GetMapping("/{id}/fields")
    public ResponseEntity<List<FieldDto>> getFieldsByCategory(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching fields for category ID: {} by user: {}", id, email);

        List<FieldDto> fields = fieldService.getFieldsByCategory(id, email);
        return ResponseEntity.ok(fields);
    }

    // ✅ Добавление поля в категорию
    @PostMapping("/{id}/fields")
    public ResponseEntity<Field> addFieldToCategory(@PathVariable Long id, @RequestBody FieldDto fieldRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Adding field to category ID: {} by user: {}", id, email);

        Field savedField = fieldService.addFieldToCategory(id, fieldRequest, email);
        return ResponseEntity.ok(savedField);
    }

    // ✅ Обновление поля в категории
    @PutMapping("/{id}/fields/{fieldId}")
    public ResponseEntity<FieldDto> updateField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody FieldDto updatedField) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🔄 Updating field ID: {} in category ID: {} for user: {}", fieldId, id, email);

        Field updated = fieldService.updateField(id, fieldId, updatedField, email);
        FieldDto fieldDto = new FieldDto(updated.getId(), updated.getName(), updated.getDescription(), updated.getFieldType(), updated.getValue());

        return ResponseEntity.ok(fieldDto);
    }

    // ✅ Удаление поля из категории
    @DeleteMapping("/{id}/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long id, @PathVariable Long fieldId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🗑 Deleting field ID: {} from category ID: {} for user: {}", fieldId, id, email);

        fieldService.deleteField(id, fieldId, email);
        return ResponseEntity.noContent().build();
    }

    // ✅ Получение group_resource_name по id категории
    @GetMapping("/{categoryId}/google-resource-name")
    public ResponseEntity<Map<String, String>> getGoogleResourceName(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User user) {

        logger.info("🔍 Запрос получения Google Resource Name для категории ID: {}, от пользователя: {}", categoryId, user.getEmail());

        // 🔥 Загружаем пользователя из базы
        User persistedUser = userRepository.findByEmail(user.getEmail()).orElse(null);

        if (persistedUser == null) {
            logger.warn("❌ Пользователь {} не найден в базе данных", user.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
        }

        // 🔍 Ищем категорию
        Category category = categoryRepository.findByIdAndUser(categoryId, persistedUser).orElse(null);

        if (category == null) {
            logger.warn("❌ Категория ID {} не найдена для пользователя {}", categoryId, user.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Категория не найдена"));
        }

        logger.info("✅ Найдена категория: {}, google_resource_name: {}", category.getName(), category.getGoogleResourceName());

        if (category.getGoogleResourceName() == null || category.getGoogleResourceName().isBlank()) {
            logger.warn("⚠️ Категория ID {} не связана с Google!", categoryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Категория не связана с Google"));
        }

        logger.info("📌 Отправляем google_resource_name: {}", category.getGoogleResourceName());
        return ResponseEntity.ok(Map.of("google_resource_name", category.getGoogleResourceName()));
    }

}