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
    private final GoogleContactsService googleContactsService;
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

        try {
            // ✅ Получаем валидный токен
            String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);

            // 🔄 Проверяем, есть ли уже такая группа в Google Contacts, если нет — создаем
            String googleResourceName = googleContactsService.createOrGetGoogleContactGroup(category.getName(), accessToken);

            if (googleResourceName != null) {
                category.setGoogleResourceName(googleResourceName);
                log.info("✅ Группа '{}' успешно создана/найдена в Google Contacts", category.getName());
            } else {
                log.warn("⚠️ Группа не найдена или не создана. Продолжаем без неё.");
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при работе с Google Contacts", e);
        }

        // ✅ Сохраняем категорию в БД только после успешного запроса к Google
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

        // 🔄 Удаляем из Google Contacts, если есть связанная группа
        if (category.getGoogleResourceName() != null) {
            try {
                String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
                googleContactsService.deleteGoogleContactGroup(category.getGoogleResourceName(), accessToken);
            } catch (Exception e) {
                log.error("❌ Ошибка при удалении категории '{}' из Google Contacts: {}", category.getName(), e.getMessage(), e);
            }
        }

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

        try {
            String accessToken = oAuth2AuthenticationService.getValidAccessToken(user);
            String googleResourceName = category.getGoogleResourceName();

            if (googleResourceName != null) {
                log.info("🔄 Обновляем группу '{}' в Google Contacts...", oldName);
                googleContactsService.updateGoogleContactGroup(googleResourceName, newName, accessToken);
            } else {
                log.warn("⚠ У категории '{}' нет связанной группы в Google Contacts. Создаём новую...", newName);
                String newGoogleResourceName = googleContactsService.createOrGetGoogleContactGroup(newName, accessToken);
                category.setGoogleResourceName(newGoogleResourceName);
                categoryRepository.save(category);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при обновлении Google Contact Group: {}", e.getMessage(), e);
        }

        log.info("✅ Категория ID={} успешно обновлена", categoryId);
        return category;
    }

}