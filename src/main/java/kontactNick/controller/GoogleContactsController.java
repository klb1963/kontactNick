package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.service.GoogleCategoryService;
import kontactNick.service.GoogleContactsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;
    private final GoogleCategoryService googleCategoryService;
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

    // ✅ Создание категории (группы) в Google Contacts
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

        String googleCategoryId = googleCategoryService.createOrGetGoogleCategory(categoryName, accessToken);

        if (googleCategoryId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create Google category"));
        }

        return ResponseEntity.ok(Map.of("googleCategoryId", googleCategoryId));
    }

    // ✅ Добавление контакта в категорию Google Contacts
    @PostMapping("/add-to-category")
    public ResponseEntity<?> addContactToCategory(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                  @RequestBody Map<String, String> request) {
        String accessToken = extractAccessToken(authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authorization header missing"));
        }

        String contactGroupId = request.get("contactGroupId");
        String contactId = request.get("contactId");

        if (contactGroupId == null || contactGroupId.isBlank() || contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Both contactGroupId and contactId are required"));
        }

        return googleContactsService.addContactToGoogleCategory(contactGroupId, contactId, accessToken);
    }

    /** ✅ Добавление контакта в Google-группу */
    @PostMapping("/contact-groups/{groupId}/add-contact")
    public ResponseEntity<?> addContactToGoogleCategory(
            @PathVariable String groupId,
            @RequestBody Map<String, String> body) {

        String contactResourceName = body.get("contactResourceName");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getGoogleAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google Access Token not found");
        }

        log.info("📡 Добавляем контакт {} в группу {} (Google Contacts) для пользователя {}",
                contactResourceName, groupId, email);

        googleContactsService.addContactToGoogleCategory(contactResourceName, "contactGroups/" + groupId, user.getGoogleAccessToken());

        return ResponseEntity.ok(Map.of("message", "✅ Контакт добавлен в Google-группу"));
    }

    // ✅ Улучшенная обработка заголовка Authorization
    private String extractAccessToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "").trim();
    }

}