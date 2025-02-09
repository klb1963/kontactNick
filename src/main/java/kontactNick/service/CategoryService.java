package kontactNick.service;

import jakarta.transaction.Transactional;
import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserGoogleTokenService userGoogleTokenService;
    private final RestTemplate restTemplate;

    // 📖получить категории пользователя
    public List<Category> getCategoriesByUserEmail(String email) {
        return categoryRepository.findByUser_Email(email);
    }

    @Transactional
    public Category createCategoryWithGoogleSync(Category category, User user) {
        log.info("📂 Создаём категорию '{}' для пользователя {}", category.getName(), user.getEmail());

        // ✅ Сначала сохраняем категорию в БД
        Category savedCategory = categoryRepository.save(category);

        // 🔄 Затем создаём группу в Google Contacts
        try {
            String accessToken = userGoogleTokenService.getValidAccessToken(user);
            String googleResourceName = createGoogleContactGroup(category.getName(), accessToken);
            savedCategory.setGoogleResourceName(googleResourceName);
            log.info("✅ Группа создана в Google Contacts: {}", googleResourceName);
        } catch (Exception e) {
            log.error("❌ Ошибка при создании группы в Google Contacts", e);
        }

        return savedCategory;
    }

    @Transactional
    public Category updateCategory(Long categoryId, String newName, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.getGoogleResourceName() != null) {
            try {
                String accessToken = userGoogleTokenService.getValidAccessToken(user);
                updateGoogleContactGroup(category.getGoogleResourceName(), newName, accessToken);
                log.info("✅ Группа в Google Contacts обновлена: {}", newName);
            } catch (Exception e) {
                log.error("❌ Ошибка при обновлении группы в Google Contacts", e);
            }
        }

        category.setName(newName);
        return categoryRepository.save(category);
    }

    /**
     * 🔄 Создание группы в Google Contacts
     */
    private String createGoogleContactGroup(String categoryName, String accessToken) {
        log.info("🔄 Создаём группу '{}' в Google Contacts...", categoryName);

        // 🔍 Проверяем, получили ли мы Access Token
        System.out.println("🔍 Access Token: " + accessToken);
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("❌ Ошибка: Access Token отсутствует!");
            return null;
        }

        String url = "https://people.googleapis.com/v1/contactGroups";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", categoryName);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String resourceName = (String) response.getBody().get("resourceName");
                log.info("✅ Группа создана в Google Contacts: {}", resourceName);
                return resourceName;
            }
            log.error("❌ Ошибка создания группы в Google Contacts: {}", response.getBody());
        } catch (Exception e) {
            log.error("❌ API Google Contacts временно недоступен", e);
        }
        return null;
    }

    /**
     * 🔄 Обновление группы в Google Contacts
     */
    private void updateGoogleContactGroup(String resourceName, String newName, String accessToken) {
        log.info("🔄 Обновляем группу '{}' в Google Contacts...", newName);

        String url = "https://people.googleapis.com/v1/contactGroups/" + resourceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", newName);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Группа '{}' обновлена в Google Contacts", newName);
            } else {
                log.error("❌ Ошибка обновления группы в Google Contacts: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ API Google Contacts временно недоступен", e);
        }
    }
}
