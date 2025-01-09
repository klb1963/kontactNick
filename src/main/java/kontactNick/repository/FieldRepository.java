package kontactNick.repository;

import kontactNick.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldRepository extends JpaRepository<Field, Long> {

    List<Field> findByCategoryId(Long categoryId);

}
