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

    // ✅ Создание объекта в корректном формате Google API
    const googleContact: any = {
      names: [{ givenName: this.contactData.name }],
      emailAddresses: [{ value: this.contactData.email }],
      contactGroupMemberships: [{
        contactGroupResourceName: this.data.category.contactGroupResourceName || `contactGroups/${this.categoryId}`
      }]
    };

    // ✅ Добавляем телефон, если есть
    if (this.contactData.phone?.trim()) {
      googleContact.phoneNumbers = [{ value: this.contactData.phone }];
    }

    // ✅ Дополнительные поля
    this.fields.forEach(field => {
      if (this.contactData.otherFields[field.name]) {
        googleContact[field.name] = this.contactData.otherFields[field.name];
      }
    });

    console.log("📤 Saving to Google Contacts:", googleContact);

    this.saveContactToGoogle(googleContact);
  }

  saveContactToGoogle(contact: any): void {
    this.googleContactsService.addToGoogleContacts(contact).subscribe({
      next: (response) => {
        console.log("✅ Contact successfully added to Google Contacts!", response);
        alert("✅ Контакт успешно добавлен в Google!");

        // ✅ Обновляем объект перед закрытием диалога
        this.contactData.contactGroupResourceName = contact.contactGroupMemberships[0].contactGroupResourceName;
        this.dialogRef.close(this.contactData);
      },
      error: (err) => {
        console.error("❌ Error adding contact to Google:", err);
        alert("❌ Ошибка при добавлении контакта в Google.");
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
