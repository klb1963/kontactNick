import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-category-dialog',
  templateUrl: './category-dialog.component.html',
  styleUrls: ['./category-dialog.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ]
})
export class CategoryDialogComponent {
  categoryName: string;
  categoryDescription: string; // ✅ Добавлено для поля описания

  constructor(
    private dialogRef: MatDialogRef<CategoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.categoryName = data?.name || '';
    this.categoryDescription = data?.description || '';  // ✅ Инициализация описания
  }

  save() {
    this.dialogRef.close({
      name: this.categoryName,
      description: this.categoryDescription // ✅ Передача описания при сохранении
    });
  }

  close() {
    this.dialogRef.close();
  }
}
