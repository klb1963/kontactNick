package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.dto.FieldDto;
import kontactNick.dto.UserProfileDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FieldRepository fieldRepository;

    @Autowired
    public ApiController(UserRepository userRepository, CategoryRepository categoryRepository, FieldRepository fieldRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.fieldRepository = fieldRepository;
    }

    // Главная страница
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }

    // Эндпойнт для профиля пользователя
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new UserProfileDto(
                        user.getNick(),
                        user.getEmail(),
                        user.getRole()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // Создание нового пользователя
    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        userRepository.save(user);
        return "User " + user.getNick() + " created!";
    }

    // Получение пользователя по email
    @GetMapping("/users/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.of(userRepository.findByEmail(email));
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryDto categoryDto) {
        // извлекаем текущего пользователя из контекста
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // находим его email
        String email = authentication.getName();
        log.debug("Authenticated email: {}", email);

        // находим собственно пользователя по его email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.debug("Found user: {} with ID: {}", user.getEmail(), user.getId());

        // Создаем категорию
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription()); // ✅ Исправлено
        category.setUser(user); // Связываем категорию с пользователем
        //сохраняем категорию
        Category savedCategory = categoryRepository.save(category);

        log.debug("Saved category '{}' for user ID: {}", savedCategory.getName(), savedCategory.getUser().getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // get categories
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(@AuthenticationPrincipal OidcUser oidcUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName(); // Получаем email из токена
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        List<Category> categories = categoryRepository.findByUser_Email(email);
        return ResponseEntity.ok(categories);
    }

    // add ONE field to category
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/categories/{categoryId}/field")
    public ResponseEntity<String> addFieldToCategory(@PathVariable Long categoryId, @RequestBody FieldDto fieldRequest) {
        log.debug("Received request to add field to category with ID: {}", categoryId);

        // Проверяем, существует ли категория
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        log.debug("Category found: {}", category.getName());

        // Создаем новое поле
        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setValue(fieldRequest.getValue());
        field.setCategory(category);

        fieldRepository.save(field);

        log.debug("Field saved successfully with ID: {}", field.getId());

        return ResponseEntity.ok("Field added to category successfully");
    }

    // add A FEW fields to category
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/categories/{categoryId}/fields")
    public ResponseEntity<String> addFieldsToCategory(@PathVariable Long categoryId, @RequestBody List<Field> fields) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        for (Field field : fields) {
            field.setCategory(category);
            fieldRepository.save(field);
        }

        return ResponseEntity.ok("Fields added to category successfully");
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @GetMapping("/categories/{categoryId}/fields")
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

    @GetMapping("/api/token")
    public ResponseEntity<String> getAuthUrl() {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code"
                + "&client_id=YOUR_CLIENT_ID"
                + "&redirect_uri=http://localhost:8080/login/oauth2/code/google"
                + "&scope=openid%20profile%20email"
                + "&state=state_value";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "http://localhost:4200");
        return ResponseEntity.ok()
                .headers(headers)
                .body(googleAuthUrl);
    }

}