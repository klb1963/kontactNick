package kontactNick.service;

import kontactNick.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCategoryService {
    private final OAuth2AuthenticationService oAuth2AuthenticationService;
    private final RestTemplate restTemplate;

    /*
    📂 Создание категории в Google Contacts
    */
    public String createOrGetGoogleCategory(String categoryName, String accessToken) {
        log.info("📂 Создаём или получаем группу '{}' в Google Contacts...", categoryName);

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("❌ Ошибка: Access Token отсутствует! Запрос невозможен.");
            return null;
        }

        try {
            String url = "https://people.googleapis.com/v1/contactGroups";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(Map.of("contactGroup", Map.of("name", categoryName)), headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String resourceName = (String) response.getBody().get("resourceName");
                log.info("✅ Группа '{}' создана в Google Contacts: {}", categoryName, resourceName);
                return resourceName;
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при создании группы в Google Contacts", e);
        }
        return null;
    }

    /*
    🗑 Удаление категории в Google Contacts
     */
    public void deleteGoogleCategory(String googleResourceName, User user) {
        if (googleResourceName == null || googleResourceName.isEmpty()) {
            log.warn("⚠ Google Contact Group resource name is null or empty. Skipping deletion.");
            return;
        }

        try {
            String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
            String url = "https://people.googleapis.com/v1/" + googleResourceName + "?deleteContacts=true";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Google Contact Group '{}' deleted successfully", googleResourceName);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при удалении Google Contact Group '{}': {}", googleResourceName, e.getMessage(), e);
        }
    }

    /*
    🔄 Обновление названия группы в Google Contacts
     */
    public void updateGoogleCategory(String googleResourceName, String newName, User user) {
        if (googleResourceName == null || googleResourceName.isEmpty()) {
            log.warn("⚠ Google Contact Group resource name is null or empty. Skipping update.");
            return;
        }

        try {
            String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
            String url = "https://people.googleapis.com/v1/" + googleResourceName;

            Map<String, Object> updateBody = Map.of(
                    "contactGroup", Map.of("name", newName),
                    "updateMask", "name"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Google Contact Group '{}' updated successfully", googleResourceName);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при обновлении Google Contact Group '{}': {}", googleResourceName, e.getMessage(), e);
        }
    }

    /**
     * 🗑 Удаление группы из Google Contacts
     */
    public void deleteGoogleContactGroup(String googleResourceName, String accessToken) {
        if (googleResourceName == null || googleResourceName.isEmpty()) {
            log.warn("⚠ Google Contact Group resource name is null or empty. Skipping deletion.");
            return;
        }

        String url = "https://people.googleapis.com/v1/" +
                (googleResourceName.startsWith("contactGroups/") ? googleResourceName : "contactGroups/" + googleResourceName) +
                "?deleteContacts=true";

        log.info("🗑 Удаляем Google Contact Group: {}", googleResourceName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Google Contact Group '{}' удалена успешно", googleResourceName);
            } else {
                log.error("❌ Ошибка удаления Google Contact Group '{}'. Код ответа: {}", googleResourceName, response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("❌ Ошибка при удалении группы ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Ошибка API Google Contacts", e);
        }
    }

}
