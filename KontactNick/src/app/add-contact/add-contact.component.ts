import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../services/category.service';
import { AuthService } from '../services/auth.service';
import { GoogleContactsService } from '@app/services/google-contacts.service';
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
  categoryId: number;
  fields: any[] = [];
  contactData: any = { firstName: '', nick: '', email: '', category: '', otherFields: {} };
  currentUserNick: string = '';

  constructor(
    private dialogRef: MatDialogRef<AddContactDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private categoryService: CategoryService,
    private authService: AuthService,
    private googleContactsService: GoogleContactsService
  ) {
    this.categoryId = data.categoryId;
  }

  ngOnInit(): void {
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

  save(): void {
    // ✅ Проверяем, есть ли поля name и email
    if (!this.contactData.name?.trim() || !this.contactData.email?.trim()) {
      alert("⚠️ Please enter a valid name and email!");
      return;
    }

    // ✅ Преобразуем названия полей в формат, нужный Google
    const googleContact = {
      name: this.contactData.name,  // Используем name вместо firstName
      nickname: this.contactData.nick || '',  // Никнейм необязателен
      email: this.contactData.email,
      phone: this.contactData.телефон || ''  // Если есть, добавляем телефон
    };

    // ✅ Все остальные данные сохраняем списком
    const additionalFields = Object.keys(this.contactData)
      .filter(key => !['name', 'nick', 'email', 'телефон'].includes(key))
      .map(key => ({ field: key, value: this.contactData[key] }));

    console.log("📤 Saving to Google Contacts:", googleContact);
    console.log("📥 Additional fields:", additionalFields);

    this.dialogRef.close(this.contactData);
    this.saveContactToGoogle(googleContact, additionalFields);
  }

  saveContactToGoogle(contact: any, additionalFields: any[]): void {
    const accessToken = localStorage.getItem("googleAccessToken");
    if (!accessToken) {
      console.error("❌ No Google access token found!");
      alert("⚠️ You need to log in with Google first!");
      return;
    }

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

  }

  close(): void {
    this.dialogRef.close();
  }
}
