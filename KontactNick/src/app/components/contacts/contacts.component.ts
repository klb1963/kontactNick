import { Component, OnInit } from '@angular/core';
import { GoogleContactsService } from '../../services/google-contacts.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-contacts',
  templateUrl: './contacts.component.html',
  standalone: true
})
export class ContactsComponent implements OnInit {  // ✅ Добавили OnInit
  constructor(
    private googleContactsService: GoogleContactsService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    // ✅ Проверяем, получаем ли токен при загрузке страницы
    this.authService.getGoogleAccessToken().subscribe(accessToken => {
      if (accessToken) {
        console.log("✅ Google Access Token успешно получен:", accessToken);
      } else {
        console.error("❌ Не удалось получить Google Access Token!");
      }
    });
  }

  saveContact() {
    const contact = {
      names: [{ givenName: 'John' }],  // ✅ Google требует массив `names`
      emailAddresses: [{ value: 'john@example.com' }], // ✅ Google требует `emailAddresses`
      phoneNumbers: [{ value: '+123456789' }] // ✅ Добавили телефон (по желанию)
    };

    // ✅ Получаем Google Access Token
    this.authService.getGoogleAccessToken().subscribe(accessToken => {
      if (!accessToken) {
        console.error("❌ No Google access token found!");
        alert("⚠️ Авторизация в Google не найдена! Перезайдите в аккаунт.");
        return;
      }

      // ✅ Отправляем контакт в Google Contacts API
      this.googleContactsService.addToGoogleContacts(contact, accessToken).subscribe({
        next: (response) => {
          console.log("✅ Contact successfully added to Google Contacts!", response);
          alert("✅ Контакт успешно добавлен в Google!");
        },
        error: (err) => {
          console.error("❌ Error adding contact to Google:", err);
          alert("❌ Ошибка при добавлении контакта в Google.");
        }
      });
    });
  }
}
