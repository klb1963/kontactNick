package kontactNick.service;

import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

        public List<Category> getCategoriesByUserEmail(String email) {
            return categoryRepository.findByUser_Email(email);
    }
}
