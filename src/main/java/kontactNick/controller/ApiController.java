package kontactNick.controller;

import kontactNick.dto.UserProfileDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import kontactNick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Главная страница API
    @GetMapping("/")
    public String apiRoot() {
        return "Welcome to KontactNick API";
    }

    // Эндпоинт для профиля пользователя
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

    // Страница входа (опционально, если нужна)
    @GetMapping("/login")
    public String login() {
        return "login"; // Отобразите страницу входа, если используете шаблоны
    }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, String>> addCategory(@AuthenticationPrincipal OidcUser oidcUser, @RequestBody String categoryName) {
        String email = oidcUser.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Category category = new Category();
        category.setName(categoryName);
        category.setUser(user);

        categoryRepository.save(category);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Category added successfully");
        return ResponseEntity.ok(response);
    }

    // add category
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(@AuthenticationPrincipal OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        List<Category> categories = categoryRepository.findByUserId(user.getId());
        return ResponseEntity.ok(categories);
    }

    // add ONE field to category
    @PostMapping("/categories/{categoryId}/field")
    public ResponseEntity<String> addFieldToCategory(@PathVariable Long categoryId, @RequestBody Field fieldRequest) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setCategory(category);

        fieldRepository.save(field);
        return ResponseEntity.ok("Field added to category successfully");
    }

    // add A FEW fields to category
    @PostMapping("/categories/{categoryId}/fields")
    public ResponseEntity<String> addFieldsToCategory(@PathVariable Long categoryId, @RequestBody List<Field> fields) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        for (Field field : fields) {
            field.setCategory(category);
            fieldRepository.save(field);
        }

        return ResponseEntity.ok("Fields added to category successfully");
    }

    @GetMapping("/api/token")
    public ResponseEntity<String> getAccessToken(@AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = oidcUser.getIdToken().getTokenValue(); // Получаем токен
        return ResponseEntity.ok(accessToken);
    }

}