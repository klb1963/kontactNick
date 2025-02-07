import { Component } from '@angular/core';
import { GoogleContactsService } from '../../services/google-contacts.service';

@Component({
  selector: 'app-contacts',
  templateUrl: './contacts.component.html',
  standalone: true
})
export class ContactsComponent {

  constructor(private googleContactsService: GoogleContactsService) {}

  saveContact() {
    const contact = {
      firstName: 'John',
      nickname: 'Johnny',
      email: 'john@example.com'
    };

    const accessToken = localStorage.getItem("googleAccessToken"); // Получаем токен после авторизации
    if (accessToken) {
      this.googleContactsService.addToGoogleContacts(contact, accessToken).subscribe(response => {
        console.log('Contact saved:', response);
      });
    } else {
      console.log("User is not authenticated!");
    }
  }
}
