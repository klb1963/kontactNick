package kontactNick.controller;

import kontactNick.service.GoogleContactsService;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;

    public GoogleContactsController(GoogleContactsService googleContactsService) {
        this.googleContactsService = googleContactsService;
    }

    @PostMapping("/contacts")
    public String addContact(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> contact) {
        try {
            String accessToken = token.replace("Bearer ", "");
            googleContactsService.addContact(accessToken, contact.get("firstName"), contact.get("lastName"), contact.get("email"));
            return "Contact added successfully!";
        } catch (IOException e) {
            return "Error adding contact: " + e.getMessage();
        }
    }

}
