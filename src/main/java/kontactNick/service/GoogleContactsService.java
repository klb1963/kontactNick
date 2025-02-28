package kontactNick.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleContactsService {

    private final RestTemplate restTemplate;

    /**
     * ✅ fetchGoogleContacts
     */
    public List<Map<String, Object>> fetchGoogleContacts(String accessToken) {
        log.info("📡 Запрос контактов из Google Contacts...");

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("❌ Access Token отсутствует! Запрос невозможен.");
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
     * ✅ Добавление контакта в группу Google Contacts
     */
    public ResponseEntity<?> addContactToGoogleCategory(String contactGroupId, String contactId, String accessToken) {
        log.info("📂 Добавляем контакт '{}' в категорию '{}' в Google Contacts...", contactId, contactGroupId);

        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Access Token отсутствует"));
        }

        if (contactGroupId == null || contactGroupId.isBlank() || contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Оба параметра contactGroupId и contactId обязательны"));
        }

        // ✅ Проверяем, уже есть ли контакт в группе
        if (isContactInGroup(contactGroupId, contactId, accessToken)) {
            log.info("⚠ Контакт '{}' уже в группе '{}'. Пропускаем добавление.", contactId, contactGroupId);
            return ResponseEntity.ok(Map.of("message", "Контакт уже добавлен в категорию"));
        }

        String url = "https://people.googleapis.com/v1/contactGroups/" + contactGroupId + "/members:modify";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("resourceNamesToAdd", List.of(contactId));
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

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
     * 🔍 Проверка, находится ли контакт в группе Google
     */
    public boolean isContactInGroup(String contactGroupId, String contactId, String accessToken) {
        log.info("🔍 Проверяем, находится ли контакт '{}' в группе '{}'", contactId, contactGroupId);

        if (contactGroupId == null || contactGroupId.isBlank() || contactId == null || contactId.isBlank()) {
            log.warn("⚠ Невозможно проверить, так как contactGroupId или contactId пустые.");
            return false;
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("⚠ Access Token отсутствует! Проверка невозможна.");
            return false;
        }

        String url = "https://people.googleapis.com/v1/contactGroups/" + contactGroupId + "?maxMembers=2000";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            while (url != null) { // Добавляем поддержку nextPageToken
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