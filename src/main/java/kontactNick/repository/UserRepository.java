package kontactNick.repository;

import kontactNick.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email); // ✅ Метод для поиска по email
    boolean existsByEmail(String email);
    Optional<User> findByNick(String nick);       // ✅ Метод для поиска по nick
}
