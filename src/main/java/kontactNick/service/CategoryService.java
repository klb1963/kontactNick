package kontactNick.service;

import jakarta.transaction.Transactional;
import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String accessToken = userGoogleTokenService.getValidAccessToken(user);
        String googleResourceName = createGoogleContactGroup(category.getName(), accessToken);
        category.setGoogleResourceName(googleResourceName);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long categoryId, String newName, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.getGoogleResourceName() != null) {
            String accessToken = userGoogleTokenService.getValidAccessToken(user);
            updateGoogleContactGroup(category.getGoogleResourceName(), newName, accessToken);
        }

        category.setName(newName);
        return categoryRepository.save(category);
    }

    private String createGoogleContactGroup(String categoryName, String accessToken) {
        String url = "https://people.googleapis.com/v1/contactGroups";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", categoryName);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("resourceName");
        }

        throw new RuntimeException("Failed to create Google Contact Group");
    }

    private void updateGoogleContactGroup(String resourceName, String newName, String accessToken) {
        String url = "https://people.googleapis.com/v1/contactGroups/" + resourceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", newName);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to update Google Contact Group");
        }

    }

}
