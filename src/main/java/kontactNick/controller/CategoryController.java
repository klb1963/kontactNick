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
import kontactNick.service.OAuth2AuthenticationService;
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
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequiredArgsConstructor
public class CategoryController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FieldRepository fieldRepository;
    private final FieldService fieldService;
    private final CategoryService categoryService;
    private final GoogleContactsService googleContactsService;

    @PostMapping("/google/contact-groups")
    public ResponseEntity<?> createGoogleCategory(@RequestBody Map<String, Object> requestBody) {
        log.info("📡 Received request to create Google category: {}", requestBody);

        // ✅ Извлекаем объект contactGroup
        Map<String, String> contactGroup = (Map<String, String>) requestBody.get("contactGroup");
        if (contactGroup == null || !contactGroup.containsKey("name")) {
            log.error("❌ Missing 'contactGroup.name' field in request: {}", requestBody);
            return ResponseEntity.badRequest().body("Missing 'contactGroup.name' field");
        }

        String categoryName = contactGroup.get("name");
        log.info("📂 Creating Google Contact Group: {}", categoryName);

        // ✅ Получаем текущего пользователя из SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("🔑 Authenticated user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User {} not found!", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // ✅ Получаем токен из базы данных
        String accessToken = user.getGoogleAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("❌ Ошибка: У пользователя {} нет Google Access Token", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google OAuth2 token is missing");
        }

        // ✅ Отправляем запрос в Google API
        try {
            String googleGroupId = googleContactsService.createOrGetGoogleContactGroup(categoryName, accessToken);
            return ResponseEntity.ok(Map.of("googleGroupId", googleGroupId));
        } catch (Exception e) {
            log.error("❌ Failed to create Google category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Google category");
        }
    }

    // ✅ Создание категории
    @PostMapping("/categories")
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

        savedCategory = categoryService.createCategoryWithGoogleSync(category, user);

        log.info("✅ Created category '{}' for user '{}'", savedCategory.getName(), email);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // ✅ Получение категории
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    // ✅ Получение всех категорий пользователя
    @GetMapping("/categories")
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
    @PostMapping("/categories/{categoryId}/field")
    public ResponseEntity<Field> addFieldToCategory(@PathVariable Long categoryId, @RequestBody FieldDto fieldRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Adding field to category ID: {} by user: {}", categoryId, email);

        // Передаём данные в сервис для добавления поля
        Field savedField = fieldService.addFieldToCategory(categoryId, fieldRequest, email);

        log.info("✅ Field '{}' added to category '{}'", savedField.getName(), savedField.getCategory().getName());

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(savedField); // Возвращаем сохранённое поле
    }

    // ✅ Обновление категории (синхронизация с Google Contacts API) - new version
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDto categoryDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(email))
                .orElseThrow(() -> {
                    log.warn("❌ Attempt to update category {} failed, user {} has no access!", categoryId, email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        if (categoryDto.getName() == null || categoryDto.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be empty");
        }

        boolean nameChanged = !category.getName().equals(categoryDto.getName());
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());

        categoryRepository.save(category);
        log.info("✅ Updated category '{}' for user '{}'", category.getName(), email);

        // 🔄 Если название изменилось, обновляем в Google Contacts
        if (nameChanged && category.getGoogleResourceName() != null) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            String accessToken = user.getGoogleAccessToken();

            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("❌ User '{}' has no Google access token, skipping Google Contacts update", email);
            } else {
                try {
                    googleContactsService.updateGoogleContactGroup(category.getGoogleResourceName(), categoryDto.getName(), accessToken);
                    log.info("✅ Google Contact Group updated: {}", category.getGoogleResourceName());
                } catch (Exception e) {
                    log.error("❌ Failed to update Google Contact Group '{}': {}", category.getGoogleResourceName(), e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(category);
    }


    // ✅ Удаление категории (с удалением группы в Google Contacts)
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(email))
                .orElseThrow(() -> {
                    log.warn("❌ Attempt to delete category {} failed, user {} has no access!", categoryId, email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        // 📡 Удаление группы в Google Contacts API, если есть googleResourceName
        if (category.getGoogleResourceName() != null) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            String accessToken = user.getGoogleAccessToken();

            if (accessToken != null && !accessToken.isEmpty()) {
                try {
                    googleContactsService.deleteGoogleContactGroup(category.getGoogleResourceName(), accessToken);
                    log.info("🗑 Deleted Google Contact Group: {}", category.getGoogleResourceName());
                } catch (Exception e) {
                    log.error("❌ Failed to delete Google Contact Group: {}", e.getMessage(), e);
                }
            } else {
                log.warn("⚠ No Google Access Token found for user {}", email);
            }
        }

        categoryRepository.delete(category);
        log.info("🗑 Deleted category '{}' for user '{}'", category.getName(), email);
        return ResponseEntity.noContent().build();
    }

    // ✅ Получение полей категории с проверкой владельца
    @GetMapping("/categories/{categoryId}/fields")
    public ResponseEntity<List<FieldDto>> getFieldsByCategory(@PathVariable Long categoryId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("📌 Fetching fields for category ID: {} by user: {}", categoryId, email);

        // Проверим наличие пользователя в контексте безопасности
        if (email == null || email.isEmpty()) {
            log.warn("❌ No authenticated user found in SecurityContext.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Поиск категории и проверка владельца
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("❌ Category {} not found in the database.", categoryId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
                });

        // 🔍 Проверка владельца категории
        boolean isOwner = category.getUser().getEmail().equals(email);
        log.debug("🔍 Ownership check for category '{}': Expected owner '{}', actual owner '{}'",
                category.getName(), email, category.getUser().getEmail());

        if (!isOwner) {
            log.warn("❌ Access denied. Category '{}' (ID: {}) does not belong to user '{}'",
                    category.getName(), categoryId, email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.debug("✅ Category '{}' (ID: {}) belongs to user '{}'", category.getName(), categoryId, email);

        // Преобразование полей в DTO для ответа
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

    // Удаление поля из категории
    @DeleteMapping("/categories/{categoryId}/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long categoryId, @PathVariable Long fieldId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Field field = fieldRepository.findById(fieldId)
                .filter(f -> f.getCategory().getId().equals(categoryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found or doesn't belong to category"));

        fieldRepository.delete(field);
        return ResponseEntity.noContent().build();
    }

    // Обновление поля в категории
    @PutMapping("/categories/{categoryId}/fields/{fieldId}")
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

}