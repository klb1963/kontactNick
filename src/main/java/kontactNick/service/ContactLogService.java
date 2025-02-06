package kontactNick.service;

import kontactNick.entity.ContactLog;
import kontactNick.repository.ContactLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContactLogService {

    private final ContactLogRepository contactLogRepository;
    private final UserService userService;

    @Autowired
    public ContactLogService(ContactLogRepository contactLogRepository, UserService userService) {
        this.contactLogRepository = contactLogRepository;
        this.userService = userService;
    }

    public void saveLog(ContactLog log) {
        // ✅ Заполняем userNick, если он не передан
        if (log.getUserNick() == null || log.getUserNick().isEmpty()) {
            log.setUserNick(userService.getCurrentUserNick()); // ✅ Заполняем, если null
        }
        log.setCreatedAt(LocalDateTime.now()); // ✅ Добавляем временную метку
        contactLogRepository.save(log);
    }
}
