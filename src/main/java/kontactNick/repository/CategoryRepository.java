package kontactNick.repository;

import kontactNick.entity.Category;
import kontactNick.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser_Email(String email);
    Optional<Category> findByIdAndUser(Long id, User user);

}
