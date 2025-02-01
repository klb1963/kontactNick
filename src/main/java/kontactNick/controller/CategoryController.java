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
import kontactNick.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    // ✅ Создание категории
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

        Category savedCategory = categoryRepository.save(category);
        log.info("✅ Created category '{}' for user '{}'", savedCategory.getName(), email);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // ✅ Получение всех категорий пользователя
    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching categories for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User {} not found!", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        List<Category> categories = categoryRepository.findByUser_Email(email);
        log.info("📂 Found {} categories for user {}", categories.size(), email);

        return ResponseEntity.ok(categories);
    }

    // ✅ Добавление одного поля в категорию (с проверкой владельца)
    @PostMapping("/{categoryId}/field")
    public ResponseEntity<String> addFieldToCategory(@PathVariable Long categoryId, @RequestBody FieldDto fieldRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Adding field to category ID: {} by user: {}", categoryId, email);

        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(email)) // Проверка владельца
                .orElseThrow(() -> {
                    log.warn("❌ Category {} not found or doesn't belong to user {}", categoryId, email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setValue(fieldRequest.getValue());
        field.setCategory(category);

        fieldRepository.save(field);
        log.info("✅ Field '{}' added to category '{}'", field.getName(), category.getName());

        return ResponseEntity.ok("Field added successfully");
    }

    // ✅ Обновление категории (с проверкой владельца)
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

        log.info("✅ Updated category '{}' for user '{}'", category.getName(), email);
        return ResponseEntity.ok(category);
    }

    // ✅ Удаление категории (с проверкой владельца)
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
        log.info("🗑 Deleted category '{}' for user '{}'", category.getName(), email);
        return ResponseEntity.noContent().build();
    }
}