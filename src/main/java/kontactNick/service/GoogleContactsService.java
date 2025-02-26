package kontactNick.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleContactsService {

    private final RestTemplate restTemplate;

    // 🔄 Получение Contacts из Google
    public List<Map<String, Object>> fetchGoogleContacts(String accessToken) {
        log.info("📡 Запрос контактов из Google Contacts...");

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("❌ Ошибка: Access Token отсутствует!");
        }

        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> contacts = (List<Map<String, Object>>) response.getBody().get("connections");
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

        return List.of(); // Возвращаем пустой список, если ничего не нашли
    }

    // 🔄 Создание или получение ID существующей группы в Google Contacts
    public String createOrGetGoogleContactGroup(String categoryName, String accessToken) {
        log.info("🔄 Создаём или получаем группу '{}' в Google Contacts...", categoryName);

        String url = "https://people.googleapis.com/v1/contactGroups";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(Map.of("contactGroup", Map.of("name", categoryName)), headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String resourceName = (String) response.getBody().get("resourceName");
                log.info("✅ Группа '{}' создана в Google Contacts: {}", categoryName, resourceName);
                return resourceName;
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("⚠️ Группа '{}' уже существует. Получаем её ID...", categoryName);
                return getExistingGoogleContactGroup(categoryName, accessToken);
            } else {
                log.error("❌ Ошибка при создании группы ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка API Google Contacts", e);
        }
        return null;
    }

    // 🔍 Получение resourceName существующей группы в Google Contacts
    public String getExistingGoogleContactGroup(String categoryName, String accessToken) {
        log.info("🔍 Ищем уже существующую группу '{}' в Google Contacts...", categoryName);

        String url = "https://people.googleapis.com/v1/contactGroups";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> contactGroups = (List<Map<String, Object>>) response.getBody().get("contactGroups");
                for (Map<String, Object> group : contactGroups) {
                    if (categoryName.equals(group.get("name"))) {
                        String resourceName = (String) group.get("resourceName");
                        log.info("✅ Найдена существующая группа '{}': {}", categoryName, resourceName);
                        return resourceName;
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка групп Google Contacts", e);
        }
        return null;
    }

    // 🔄 Создаем группу в Google Contacts
    public ResponseEntity<?> createGoogleContactGroup(String categoryName, String accessToken) {
        log.info("📂 Создаём категорию '{}' в Google Contacts...", categoryName);

        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Access Token отсутствует"));
        }

        String url = "https://people.googleapis.com/v1/contactGroups";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ Формируем JSON-запрос
        Map<String, Object> requestBody = Map.of("contactGroup", Map.of("name", categoryName));
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String resourceName = (String) response.getBody().get("resourceName");
                log.info("✅ Категория '{}' создана в Google Contacts: {}", categoryName, resourceName);
                return ResponseEntity.ok(Map.of("googleGroupId", resourceName));
            } else {
                log.error("❌ Ошибка создания категории: {}", response.getBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка создания категории в Google Contacts"));
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("⚠️ Категория '{}' уже существует в Google Contacts. Получаем её ID...", categoryName);
                String existingGroupId = getExistingGoogleContactGroup(categoryName, accessToken);
                if (existingGroupId != null) {
                    return ResponseEntity.ok(Map.of("googleGroupId", existingGroupId));
                } else {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Категория уже существует, но не найдена"));
                }
            } else {
                log.error("❌ Ошибка при создании категории ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getResponseBodyAsString()));
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при обращении к Google Contacts API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "API временно недоступен"));
        }
    }

    // 🔄 Удаляем группу из Google Contacts
    public void deleteGoogleContactGroup(String googleResourceName, String accessToken) {
        if (googleResourceName == null || googleResourceName.isEmpty()) {
            log.warn("⚠ Google Contact Group resource name is null or empty. Skipping deletion.");
            return;
        }

        String url = "https://people.googleapis.com/v1/" + googleResourceName;
        log.info("🗑 Sending DELETE request to Google Contacts API: {}", url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("✅ Google Contact Group '{}' deleted successfully", googleResourceName);
            } else {
                log.error("❌ Failed to delete Google Contact Group '{}'. Response: {}", googleResourceName, response.body());
            }
        } catch (Exception e) {
            log.error("❌ Error deleting Google Contact Group '{}': {}", googleResourceName, e.getMessage(), e);
        }
    }

    // 🔄 Обновляем группу в Google Contacts - 3rd version
    public void updateGoogleContactGroup(String googleResourceName, String newName, String accessToken) {
        if (googleResourceName == null || googleResourceName.isEmpty()) {
            log.warn("⚠ Google Contact Group resource name is null or empty. Skipping update.");
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            log.warn("⚠ New name for Google Contact Group is null or empty. Skipping update.");
            return;
        }

        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.warn("⚠ Access token is missing. Cannot update Google Contact Group.");
            return;
        }

        String url = "https://people.googleapis.com/v1/" + googleResourceName;
        log.info("🔄 Checking if Google Contact Group '{}' exists before updating...", googleResourceName);

        try {
            // 🔎 Проверяем, существует ли группа
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

            if (getResponse.statusCode() == 404) {
                log.warn("❌ Google Contact Group '{}' not found. Creating a new group...", googleResourceName);

                // 🆕 Создаём новую группу
                String createUrl = "https://people.googleapis.com/v1/contactGroups";
                Map<String, Object> createBody = Map.of(
                        "contactGroup", Map.of("name", newName)
                );

                HttpRequest createRequest = HttpRequest.newBuilder()
                        .uri(URI.create(createUrl))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(createBody)))
                        .build();

                HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

                if (createResponse.statusCode() == 200) {
                    Map<String, Object> responseMap = new ObjectMapper().readValue(createResponse.body(), Map.class);
                    googleResourceName = (String) responseMap.get("resourceName");
                    log.info("✅ Created new Google Contact Group '{}'", googleResourceName);

                    // ✅ Здесь можно обновить `googleResourceName` в БД
                } else {
                    log.error("❌ Failed to create Google Contact Group. Response: {}", createResponse.body());
                    return;
                }
            }

            // 🛠 Теперь обновляем название группы
            log.info("🔄 Updating Google Contact Group '{}': new name '{}'", googleResourceName, newName);

            Map<String, Object> updateBody = Map.of(
                    "contactGroup", Map.of("name", newName),
                    "updateMask", "name"
            );

            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(updateBody)))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());

            if (updateResponse.statusCode() == 200) {
                log.info("✅ Google Contact Group '{}' updated successfully", googleResourceName);
            } else {
                log.error("❌ Failed to update Google Contact Group '{}'. Response {}: {}",
                        googleResourceName, updateResponse.statusCode(), updateResponse.body());
            }

        } catch (Exception e) {
            log.error("❌ Error updating Google Contact Group '{}': {}", googleResourceName, e.getMessage(), e);
        }
    }

    // 🔄 Добавляем контакт в категорию в Google
    public ResponseEntity<?> addContactToGoogleCategory(String categoryId, String contactId, String accessToken) {
        log.info("📂 Добавляем контакт '{}' в категорию '{}' в Google Contacts...", contactId, categoryId);

        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Access Token отсутствует"));
        }

        if (categoryId == null || categoryId.isBlank() || contactId == null || contactId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Оба параметра categoryId и contactId обязательны"));
        }

        String url = "https://people.googleapis.com/v1/contactGroups/" + categoryId + "/members:modify";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ Формируем JSON-запрос
        Map<String, Object> requestBody = Map.of(
                "resourceNamesToAdd", List.of(contactId)
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Контакт '{}' добавлен в категорию '{}'", contactId, categoryId);
                return ResponseEntity.ok(Map.of("message", "Контакт успешно добавлен в категорию"));
            } else {
                log.error("❌ Ошибка добавления контакта: {}", response.getBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка добавления контакта в категорию"));
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Ошибка при добавлении контакта ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("❌ Ошибка при обращении к Google Contacts API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "API временно недоступен"));
        }
    }

}
