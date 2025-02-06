import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../services/category.service';
import { AuthService } from '../services/auth.service'; // ✅ Добавлен импорт AuthService
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // ✅ Для ngModel и ngForm
import { MatFormFieldModule } from '@angular/material/form-field'; // ✅ Для mat-form-field
import { MatInputModule } from '@angular/material/input'; // ✅ Для matInput
import { MatButtonModule } from '@angular/material/button'; // ✅ Для кнопок
import { MatDialogModule } from '@angular/material/dialog'; // ✅ Для работы с диалогами

@Component({
  selector: 'app-add-contact-dialog', // ✅ Используем правильный селектор
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
  contactData: any = { nick: '', currentUserNick: '' }; // ✅ Никнеймы сразу инициализируем

  constructor(
    private dialogRef: MatDialogRef<AddContactDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private categoryService: CategoryService,
    private authService: AuthService // ✅ Добавляем AuthService
  ) {
    this.categoryId = data.categoryId;
  }

  ngOnInit(): void {
    // ✅ Получаем ник текущего пользователя
    this.authService.getUserProfile().subscribe(profile => {
      if (profile && profile.nick) {
        this.contactData.currentUserNick = profile.nick;
      }
    });

    // ✅ Загружаем поля категории
    this.loadFields();
  }

  loadFields(): void {
    // ✅ Используем API-запрос вместо заглушки
    this.categoryService.getCategoryFields(this.categoryId).subscribe({
      next: (fields) => {
        this.fields = fields;
        console.log("✅ Fields loaded:", fields);
      },
      error: (err) => {
        console.error("❌ Error loading fields:", err);
      }
    });
  }

  save(): void {
    // ✅ Добавляем проверку на заполненность ника перед сохранением
    if (!this.contactData.nick.trim()) {
      alert("⚠️ Please enter a valid nick!");
      return;
    }

    console.log('📤 Saving contact:', this.contactData);
    this.dialogRef.close(this.contactData);
  }

  close(): void {
    this.dialogRef.close();
  }
}
