package kontactNick.service;

import kontactNick.entity.Category;
import kontactNick.entity.User;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<Category> getCategoriesByUserEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException("User not found with email: " + email));

        return categoryRepository.findByUser(user);
    }
}
