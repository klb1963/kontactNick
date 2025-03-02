package kontactNick.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleContactsService {

    private final RestTemplate restTemplate;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;
    private final CategoryRepository categoryRepository;

    /**
     * ✅ Добавление контакта в группу Google Contacts
     */
    public ResponseEntity<?> addContactToGoogleCategory(User user, Long categoryId, String contactId) {
        log.info("📂 Добавляем контакт '{}' в категорию ID: '{}' для пользователя '{}'", contactId, categoryId, user.getEmail());
        // ✅ Получаем access_token для пользователя
        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);  // ✅ Проверяет и обновляет токен
            log.info("🔑 Текущий Google Access Token перед отправкой: {}", accessToken);
        } catch (IllegalStateException e) {
            log.error("❌ Ошибка получения Access Token для {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Не удалось получить Access Token"));
        }

        // 🔍 Ищем категорию в БД
        Category category = categoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена"));

        // ✅ Достаём `google_resource_name`
        String contactGroupId = category.getGoogleResourceName();
        if (contactGroupId == null || contactGroupId.isBlank()) {
            log.error("❌ Ошибка: у категории '{}' (ID: {}) отсутствует google_resource_name!", category.getName(), categoryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Категория не привязана к Google"));
        }

        // ✅ Получаем access_token для пользователя
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);  // ✅ Проверяет и обновляет токен
        } catch (IllegalStateException e) {
            log.error("❌ Ошибка получения Access Token для {}: {}", user.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Не удалось получить Access Token"));
        }

        log.info("🔄 Новый Access Token получен и сохранён: {}", accessToken);

        // ❌ Проверяем, не пустые ли значения
        if (contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "contactId обязателен"));
        }

        // ✅ Проверяем, уже есть ли контакт в группе
        if (isContactInGroup(user, contactId, accessToken)) {
            log.info("⚠ Контакт '{}' уже в группе '{}'. Пропускаем добавление.", contactId, contactGroupId);
            return ResponseEntity.ok(Map.of("message", "Контакт уже добавлен в категорию"));
        }

        // 📡 Формируем запрос к Google API
        String url = "https://people.googleapis.com/v1/contactGroups/" + contactGroupId + "/members:modify";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("resourceNamesToAdd", List.of(contactId));
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        log.info("📡 Отправляем запрос на добавление контакта в Google: {}", requestBody);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Контакт '{}' добавлен в категорию '{}'", contactId, contactGroupId);
                return ResponseEntity.ok(Map.of("message", "Контакт успешно добавлен в категорию"));
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Ошибка добавления контакта ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getResponseBodyAsString()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка добавления контакта"));
    }

    /**
     * ✅ Получение списка контактов из Google Contacts
     */
    public List<Map<String, Object>> fetchGoogleContacts(User user) {
        log.info("📡 Запрос контактов из Google Contacts для пользователя {}", user.getEmail());

        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            log.error("❌ Ошибка получения Access Token для {}: {}", user.getEmail(), e.getMessage());
            return List.of();
        }

        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> contacts = (List<Map<String, Object>>) response.getBody().getOrDefault("connections", List.of());
                log.info("✅ Загружено {} контактов из Google Contacts.", contacts.size());
                return contacts;
            } else {
                log.warn("⚠️ Не удалось загрузить контакты из Google. Response: {}", response.getBody());
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Ошибка при получении контактов ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ API Google Contacts временно недоступен", e);
        }

        return List.of();
    }

    /**
     * 🔍 Проверка, находится ли контакт в группе Google
     */
    public boolean isContactInGroup(User user, String contactGroupId, String contactId) {
        log.info("🔍 Проверяем, находится ли контакт '{}' в группе '{}' для пользователя '{}'", contactId, contactGroupId, user.getEmail());

        if (contactGroupId == null || contactGroupId.isBlank() || contactId == null || contactId.isBlank()) {
            log.warn("⚠ Невозможно проверить, так как contactGroupId или contactId пустые.");
            return false;
        }

        String accessToken;
        try {
            accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
        } catch (IllegalStateException e) {
            log.error("❌ Ошибка получения Access Token для {}: {}", user.getEmail(), e.getMessage());
            return false;
        }

        String url = "https://people.googleapis.com/v1/contactGroups/" + contactGroupId + "?maxMembers=2000";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            while (url != null) { // Поддержка nextPageToken
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<String> members = (List<String>) response.getBody().getOrDefault("memberResourceNames", List.of());

                    if (members.contains(contactId)) {
                        log.info("✅ Контакт '{}' найден в группе '{}'", contactId, contactGroupId);
                        return true;
                    }

                    url = (String) response.getBody().get("nextPageToken");
                    if (url != null) {
                        url = "https://people.googleapis.com/v1/contactGroups/" + contactGroupId + "?maxMembers=2000&pageToken=" + url;
                    }
                } else {
                    break;
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Ошибка при проверке контакта в группе ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Ошибка API Google Contacts", e);
        }

        return false;
    }

}