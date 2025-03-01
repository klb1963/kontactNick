import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../services/category.service';
import { AuthService } from '../services/auth.service';
import { GoogleContactsService } from '../services/google-contacts.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-add-contact-dialog',
  templateUrl: './add-contact.component.html',
  standalone: true,
  styleUrls: ['./add-contact.component.css'],
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDialogModule
  ]
})
export class AddContactDialogComponent implements OnInit {
  categoryId: number = 0;
  categoryName: string = '';
  fields: any[] = [];
  contactData: any = { name: '', nick: '', email: '', phone: '', otherFields: {} };
  currentUserNick: string = '';

  constructor(
    public dialogRef: MatDialogRef<AddContactDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private categoryService: CategoryService,
    private authService: AuthService,
    private googleContactsService: GoogleContactsService
  ) {
    console.log("📥 Received data in constructor:", data);

    if (typeof data?.category === 'object') {
      this.categoryId = data.category.id ?? 0;
      this.categoryName = data.category.name ?? 'Unknown';
    } else {
      console.error("❌ Ошибка: `category` передан как строка, а не объект!", data.category);
    }
  }

  ngOnInit(): void {
    console.log("🛠️ Dialog opened with category:", this.categoryName, "ID:", this.categoryId);

    // ✅ Получаем ник текущего пользователя
    this.authService.getUserProfile().subscribe(profile => {
      if (profile?.nick) {
        this.currentUserNick = profile.nick;
      }
    });

    // ✅ Загружаем поля категории
    this.loadFields();
  }

  loadFields(): void {
    if (!this.categoryId) {
      console.error("❌ Ошибка: categoryId не передан!");
      return;
    }

    console.log("📡 Fetching fields for categoryId:", this.categoryId);

    this.categoryService.getCategoryFields(this.categoryId).subscribe({
      next: (fields) => {
        this.fields = fields;
        console.log("✅ Fields loaded:", fields);
      },
      error: (err: any) => {
        console.error("❌ Error loading fields:", err);
      }
    });
  }

// 🚀 Что изменилось?
// ✅ Теперь контакт привязывается к категории в Google Contacts (contactGroupMemberships).
// ✅ Используем contactGroupResourceName, если он есть, иначе формируем contactGroups/${categoryId}.
// ✅ Перед закрытием диалога обновляем this.contactData, чтобы передать категорию обратно.

  save(): void {
    if (!this.contactData.name?.trim() || !this.contactData.email?.trim()) {
      alert("⚠️ Please enter a valid name and email!");
      return;
    }

    console.log("🔄 Проверяем access_token перед отправкой контакта...");
    this.authService.getGoogleAccessToken().subscribe(accessToken => {
      if (!accessToken) {
        console.error("❌ Ошибка: нет валидного Google Access Token!");
        alert("⚠️ Ошибка аутентификации! Войдите через Google.");
        return;
      }

      console.log("✅ Используем Google Access Token:", accessToken);

      // ✅ Формируем объект контакта для Google API
      const googleContact: any = {
        names: [{ givenName: this.contactData.name }],
        emailAddresses: [{ value: this.contactData.email }],
        memberships: [{  // ⬅️ Исправлено: contactGroupMemberships → memberships
          contactGroupMembership: {
            contactGroupResourceName: this.data.category.contactGroupResourceName || `contactGroups/${this.categoryId}`
          }
        }]
      };

      // ✅ Добавляем телефон, если указан
      if (this.contactData.phone?.trim()) {
        googleContact.phoneNumbers = [{ value: this.contactData.phone }];
      }

      // ✅ Обрабатываем дополнительные поля пользователя (userDefined)
      console.log("📌 Доступные поля категории:", this.fields);
      console.log("📌 Доп. поля в contactData:", this.contactData.otherFields);

      if (this.fields.length > 0) {
        googleContact.userDefined = this.fields
          .map(field => {
            const fieldValue = this.contactData.otherFields[field.name]?.trim();
            console.log(`🔍 Обрабатываем поле: ${field.name}, значение: ${fieldValue}`);

            return {
              key: field.name,
              value: fieldValue || ''
            };
          })
          .filter(field => field.value !== ''); // убираем пустые

        if (googleContact.userDefined.length === 0) {
          delete googleContact.userDefined;
        }
      }

      // ✅ Отправляем контакт в Google API (только один раз!)
      console.log("📤 Итоговый JSON перед отправкой:", googleContact);
      this.saveContactToGoogle(googleContact);
    });
  }

  saveContactToGoogle(contact: any): void {
    this.googleContactsService.addToGoogleContacts(contact).subscribe({
      next: (response) => {
        console.log("✅ Contact created in Google Contacts!", response);

        if (response && response.resourceName) {
          console.log("🔗 Contact resourceName:", response.resourceName);

          // ✅ Добавляем контакт в выбранную категорию
          const groupResourceName = this.data.category.contactGroupResourceName || `contactGroups/${this.categoryId}`;
          console.log("📂 Adding contact to category:", groupResourceName);

          this.googleContactsService.addContactToGoogleCategory(response.resourceName, groupResourceName).subscribe({
            next: () => {
              console.log("✅ Contact successfully added to Google Category!");
              alert("✅ Контакт успешно добавлен в Google и привязан к категории!");

              this.contactData.contactGroupResourceName = groupResourceName;
              this.dialogRef.close(this.contactData);
            },
            error: (err) => {
              console.error("❌ Error adding contact to category:", err);
              alert("❌ Контакт создан, но не добавлен в категорию.");
            }
          });
        } else {
          console.error("❌ Ошибка: Google API не вернул resourceName!", response);
          alert("❌ Ошибка: Google API не вернул resourceName.");
        }
      },
      error: (err) => {
        console.error("❌ Error creating contact:", err);
        alert("❌ Ошибка при добавлении контакта в Google.");
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
