package kontactNick.service;

import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GoogleCategoryService googleCategoryService;
    private final OAuth2AuthenticationService oAuth2AuthenticationService;

    // 📖 Получение категорий пользователя
    public List<Category> getCategoriesByUserEmail(String email) {
        return categoryRepository.findByUser_Email(email);
    }

    /*
    📂 Создание категории (с синхронизацией с Google Contacts)
     */
    @Transactional
    public Category createCategoryWithGoogleSync(Category category, User user) {
        log.info("📂 Создаём категорию '{}' для пользователя {}", category.getName(), user.getEmail());

        // ✅ Получаем валидный токен
        String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);

        // 🔄 Создаём или получаем группу в Google Contacts
        String googleResourceName = googleCategoryService.createOrGetGoogleCategory(category.getName(), accessToken);


        if (googleResourceName != null) {
            category.setGoogleResourceName(googleResourceName);
        }

        return categoryRepository.save(category);
    }

    /*
    📂 Удаление категории (с синхронизацией с Google Contacts)
     */
    @Transactional
    public void deleteCategoryWithGoogleSync(Long categoryId, User user) {
        log.info("🗑 Удаляем категорию ID={} для пользователя {}", categoryId, user.getEmail());

        // 🔍 Ищем категорию в БД
        Category category = categoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена"));

        // 🔄 Удаляем из Google Contacts
        googleCategoryService.deleteGoogleCategory(category.getGoogleResourceName(), user);

        // 🗑 Удаляем из БД
        categoryRepository.delete(category);
        log.info("✅ Категория '{}' удалена из БД", category.getName());
    }

    /*
    📂 Обновление категории (с синхронизацией с Google Contacts)
     */
    @Transactional
    public Category updateCategoryWithGoogleSync(Long categoryId, String newName, User user) {
        log.info("🔄 Обновляем категорию ID={} для пользователя {}", categoryId, user.getEmail());

        // ✅ Ищем категорию в БД
        Category category = categoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена"));

        String oldName = category.getName();
        category.setName(newName);
        category = categoryRepository.save(category);

        // 🔄 Обновляем группу в Google Contacts
        googleCategoryService.updateGoogleCategory(category.getGoogleResourceName(), newName, user);

        return category;
    }
}