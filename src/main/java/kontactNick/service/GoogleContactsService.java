package kontactNick.service;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Nickname;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.Name;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Collections;

@Service
public class GoogleContactsService {

    private final GooglePeopleServiceFactory peopleServiceFactory;

    public GoogleContactsService(GooglePeopleServiceFactory peopleServiceFactory) {
        this.peopleServiceFactory = peopleServiceFactory;
    }

    public void addContact(String accessToken, String firstName, String nickname, String email) throws IOException {
        // Создаём PeopleService внутри метода
        PeopleService peopleService = peopleServiceFactory.createPeopleService(accessToken);

        Person contact = new Person();

        if (firstName != null) {
            contact.setNames(Collections.singletonList(new Name().setGivenName(firstName)));
        }

        if (nickname != null) {
            contact.setNicknames(Collections.singletonList(new Nickname().setValue(nickname)));
        }

        if (email != null) {
            contact.setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(email)));
        }

        peopleService.people().createContact(contact).execute();
    }

}
