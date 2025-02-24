package kontactNick.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleContactsService {

    private final RestTemplate restTemplate;
    private static final String GOOGLE_API_URL = "https://people.googleapis.com/v1";

    /**
     * Получение списка контактов из Google.
     */
    public ResponseEntity<String> getContacts(String accessToken) {
        String url = GOOGLE_API_URL + "/people/me/connections?personFields=names,emailAddresses,phoneNumbers";
        return sendGetRequest(url, accessToken);
    }

    /**
     * Создание новой категории (группы) в Google Contacts.
     */
    public ResponseEntity<String> createCategory(String categoryName, String accessToken) {
        String url = GOOGLE_API_URL + "/contactGroups";
        Map<String, String> requestBody = Map.of("name", categoryName);
        return sendPostRequest(url, accessToken, requestBody);
    }

    /**
     * Добавление контакта в категорию.
     */
    public ResponseEntity<String> addToCategory(String categoryId, String contactResourceName, String accessToken) {
        String url = GOOGLE_API_URL + "/contactGroups/" + categoryId + "/members:modify";
        Map<String, List<String>> requestBody = Map.of("resourceNamesToAdd", List.of(contactResourceName));
        return sendPostRequest(url, accessToken, requestBody);
    }

    private ResponseEntity<String> sendGetRequest(String url, String accessToken) {
        HttpHeaders headers = createHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    private ResponseEntity<String> sendPostRequest(String url, String accessToken, Object body) {
        HttpHeaders headers = createHeaders(accessToken);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

}
