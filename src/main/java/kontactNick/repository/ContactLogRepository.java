package kontactNick.repository;

import kontactNick.entity.ContactLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactLogRepository extends JpaRepository<ContactLog, Long> {

}
