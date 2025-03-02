package kontactNick.controller;

import kontactNick.dto.CategoryDto;
import kontactNick.entity.User;
import kontactNick.repository.UserRepository;
import kontactNick.service.GoogleCategoryService;
import kontactNick.service.GoogleContactsService;
import kontactNick.service.OAuth2AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    // ✅ Получение токена из БД пользователя
    @GetMapping("/token")
    public ResponseEntity<?> getGoogleAccessToken(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Пользователь не аутентифицирован"));
        }

        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Ошибка получения Access Token: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    // ✅ Получение списка контактов из Google Contacts
    @GetMapping("/contacts")
    public ResponseEntity<?> getGoogleContacts(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Пользователь не аутентифицирован"));
        }

        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Ошибка получения Access Token: " + e.getMessage()));
        }

        return ResponseEntity.ok(googleContactsService.fetchGoogleContacts(user)); // 🛠️ Передаём accessToken
    }

    // ✅ Создание категории (группы) в Google Contacts
    @PostMapping("/categories")
    public ResponseEntity<?> createGoogleCategory(@AuthenticationPrincipal User user,
                                                  @RequestBody Map<String, String> request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Пользователь не аутентифицирован"));
        }

        String categoryName = request.get("name");
        if (categoryName == null || categoryName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category name is required"));
        }

        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Ошибка получения Access Token: " + e.getMessage()));
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
    public ResponseEntity<?> addContactToCategory(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не аутентифицирован"));
        }

        // 🔹 Берём ID категории из запроса
        String categoryIdStr = request.get("categoryId");
        String contactId = request.get("contactId");

        // 🔹 Проверяем наличие параметров
        if (categoryIdStr == null || categoryIdStr.isBlank() || contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Оба параметра categoryId и contactId обязательны"));
        }

        try {
            Long categoryId = Long.parseLong(categoryIdStr); // ✅ Преобразуем в Long
            return googleContactsService.addContactToGoogleCategory(user, categoryId, contactId);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Неверный формат categoryId"));
        }
    }

    /** ✅ Добавление контакта в Google-группу */
    @PostMapping("/contact-groups/{groupId}/contacts")
    public ResponseEntity<?> addContactToGoogleCategory(
            @PathVariable String groupId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не аутентифицирован"));
        }

        // 📌 Проверяем, есть ли в теле запроса нужный ключ
        if (!body.containsKey("contactResourceName")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'contactResourceName' in request body"));
        }

        // 📌 Корректно извлекаем значение
        String contactResourceName = body.get("contactResourceName");
        if (contactResourceName == null || contactResourceName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid contactResourceName format"));
        }

        log.info("📡 Добавляем контакт {} в группу {} (Google Contacts) для пользователя {}",
                contactResourceName, groupId, user.getEmail());

        // ✅ Передаём user и корректные аргументы
        return googleContactsService.addContactToGoogleCategory(user, Long.valueOf(groupId), contactResourceName);
    }
}