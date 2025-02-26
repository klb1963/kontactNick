package kontactNick.controller;

import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.service.GoogleContactsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;
    private final UserRepository userRepository;

    // ✅ Получение токена из БД пользователя
    @GetMapping("/token")
    public ResponseEntity<?> getGoogleAccessToken() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No Google Access Token found"));
        }

        return ResponseEntity.ok(Map.of("accessToken", user.getGoogleAccessToken()));
    }

    // ✅ Получение списка контактов из Google Contacts
    @GetMapping("/contacts")
    public ResponseEntity<?> getGoogleContacts(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = extractAccessToken(authHeader);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authorization header missing"));
        }

        return ResponseEntity.ok(googleContactsService.fetchGoogleContacts(accessToken));
    }

    // ✅ Создание категории в Google Contacts
    @PostMapping("/categories")
    public ResponseEntity<?> createGoogleCategory(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                  @RequestBody Map<String, String> request) {
        String accessToken = extractAccessToken(authHeader);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authorization header missing"));
        }

        String categoryName = request.get("name");
        if (categoryName == null || categoryName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category name is required"));
        }

        return googleContactsService.createGoogleContactGroup(categoryName, accessToken);
    }

    // ✅ Добавление контакта в категорию Google Contacts
    @PostMapping("/add-to-category")
    public ResponseEntity<?> addContactToCategory(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                  @RequestBody Map<String, String> request) {
        String accessToken = extractAccessToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authorization header missing"));
        }

        String categoryId = request.get("categoryId");
        String contactId = request.get("contactId");

        if (categoryId == null || categoryId.isBlank() || contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Both categoryId and contactId are required"));
        }

        return googleContactsService.addContactToGoogleCategory(categoryId, contactId, accessToken);
    }

    // ✅ Улучшенная обработка заголовка Authorization
    private String extractAccessToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "").trim();
    }

}