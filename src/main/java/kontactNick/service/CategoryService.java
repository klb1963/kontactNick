package kontactNick.service;

import jakarta.transaction.Transactional;
import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GoogleContactsService googleContactsService;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    // 📖 Получение категорий пользователя
    public List<Category> getCategoriesByUserEmail(String email) {
        return categoryRepository.findByUser_Email(email);
    }

    // 📂 Создание категории (с синхронизацией с Google Contacts)
    @Transactional
    public Category createCategoryWithGoogleSync(Category category, User user) {
        log.info("📂 Создаём категорию '{}' для пользователя {}", category.getName(), user.getEmail());

        // ✅ Сохраняем категорию в БД
        Category savedCategory = categoryRepository.save(category);

        // 🔄 Проверяем, есть ли такая группа в Google Contacts
        try {
            String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
            String googleResourceName = googleContactsService.createOrGetGoogleContactGroup(category.getName(), accessToken);

            if (googleResourceName != null) {
                savedCategory.setGoogleResourceName(googleResourceName);
                log.info("✅ Группа '{}' успешно привязана к категории", category.getName());
            } else {
                log.warn("⚠️ Группа не найдена или не создана. Продолжаем без неё.");
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при работе с Google Contacts", e);
        }

        return savedCategory;
    }

//    // 🔄 Обновление категории (и группы в Google Contacts)
//    @Transactional
//    public Category updateCategory(Long categoryId, String newName, User user) {
//        Category category = categoryRepository.findById(categoryId)
//                .orElseThrow(() -> new RuntimeException("Category not found"));
//
//        // 🔄 Если категория уже связана с Google, обновляем её
//        if (category.getGoogleResourceName() != null) {
//            try {
//                String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
//                googleContactsService.updateGoogleContactGroup(category.getGoogleResourceName(), newName, accessToken);
//                log.info("✅ Группа '{}' в Google Contacts обновлена", newName);
//            } catch (Exception e) {
//                log.error("❌ Ошибка при обновлении группы в Google Contacts", e);
//            }
//        }
//
//        category.setName(newName);
//        return categoryRepository.save(category);
//    }

}