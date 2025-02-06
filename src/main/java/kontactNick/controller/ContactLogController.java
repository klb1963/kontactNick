package kontactNick.controller;

import kontactNick.entity.ContactLog;
import kontactNick.service.ContactLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact-log")
public class ContactLogController {

    private final ContactLogService contactLogService;

    @Autowired
    public ContactLogController(ContactLogService contactLogService) {
        this.contactLogService = contactLogService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> saveLog(@RequestBody ContactLog log) {
        System.out.println("📌 Saving log: " + log);
        contactLogService.saveLog(log);
        return ResponseEntity.ok().build();
    }

}
