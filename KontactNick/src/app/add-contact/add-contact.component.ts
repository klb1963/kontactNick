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
  contactData: any = { name: '', nick: '', email: '', phone: '', category: '', otherFields: {} };
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
    if (!this.contactData.name?.trim() || !this.contactData.email?.trim()) {
      alert("⚠️ Please enter a valid name and email!");
      return;
    }

    // ✅ Создание объекта для Google API
    const googleContact: any = {
      name: this.contactData.name,
      nickname: this.contactData.nick || '',
      email: this.contactData.email
    };

    // ✅ Добавляем телефон, если есть
    if (this.contactData.phone?.trim()) {
      googleContact.phoneNumbers = [{ value: this.contactData.phone }];
    }

    // ✅ Все остальные поля сохраняем списком
    const additionalFields = Object.keys(this.contactData)
      .filter(key => !['name', 'nick', 'email', 'phone'].includes(key))
      .map(key => ({ field: key, value: this.contactData[key] }));

    console.log("📤 Saving to Google Contacts:", googleContact);
    console.log("📥 Additional fields:", additionalFields);

    this.saveContactToGoogle(googleContact, additionalFields);
  }

  saveContactToGoogle(contact: any, additionalFields: any[]): void {
    this.authService.getGoogleAccessToken().subscribe((accessToken) => {  // ✅ Ждём, пока токен будет получен
      if (!accessToken) {
        console.error("❌ No Google access token found!");
        alert("⚠️ You need to log in with Google first!");
        return;
      }

      this.googleContactsService.addToGoogleContacts(contact, accessToken).subscribe({
        next: (response) => {
          console.log("✅ Contact successfully added to Google Contacts!", response);
          alert("✅ Контакт успешно добавлен в Google!");
          this.dialogRef.close(this.contactData); // ✅ Закрываем только после успеха!
        },
        error: (err) => {
          console.error("❌ Error adding contact to Google:", err);
          alert("❌ Ошибка при добавлении контакта в Google.");
        }
      });
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
