import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../services/category.service';
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
export class AddContactDialogComponent implements OnInit { // ✅ Исправлено имя класса
  categoryId: number;
  fields: any[] = []; // ✅ Переменная для хранения полей
  contactData: any = {}; // Данные, которые будут введены пользователем

  constructor(
    private dialogRef: MatDialogRef<AddContactDialogComponent>, // ✅ Исправлено имя класса
    @Inject(MAT_DIALOG_DATA) public data: any,
    private categoryService: CategoryService
  ) {
    this.categoryId = data.categoryId;
  }

  ngOnInit(): void {
    this.loadFields();
  }

  // ✅ Метод для загрузки полей категории
  loadFields(): void {
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
    console.log('📤 Saving contact:', this.contactData);
    this.dialogRef.close(this.contactData);
  }

  close(): void {
    this.dialogRef.close();
  }
}
