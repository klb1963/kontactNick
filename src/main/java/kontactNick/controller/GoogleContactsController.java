package kontactNick.controller;

import kontactNick.service.GoogleContactsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;

    @GetMapping("/contacts")
    public ResponseEntity<?> getGoogleContacts(@RequestHeader("Authorization") String authHeader) {
        String accessToken = extractAccessToken(authHeader);
        return googleContactsService.getContacts(accessToken);
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createGoogleCategory(@RequestHeader("Authorization") String authHeader,
                                                  @RequestBody Map<String, String> request) {
        String accessToken = extractAccessToken(authHeader);
        return googleContactsService.createCategory(request.get("name"), accessToken);
    }

    @PostMapping("/add-to-category")
    public ResponseEntity<?> addContactToCategory(@RequestHeader("Authorization") String authHeader,
                                                  @RequestBody Map<String, String> request) {
        String accessToken = extractAccessToken(authHeader);
        return googleContactsService.addToCategory(request.get("categoryId"), request.get("contactId"), accessToken);
    }

    private String extractAccessToken(String authHeader) {
        return authHeader.replace("Bearer ", "");
    }

}
