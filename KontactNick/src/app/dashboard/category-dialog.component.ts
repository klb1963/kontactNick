import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-category-dialog',
  templateUrl: './category-dialog.component.html',
  standalone: true,
  styleUrls: ['./category-dialog.component.css']
})
export class CategoryDialogComponent {
  categoryName: string = '';

  constructor(
    public dialogRef: MatDialogRef<CategoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private authService: AuthService
  ) {
    if (data) {
      this.categoryName = data.name;
    }
  }

  saveCategory() {
    if (!this.categoryName.trim()) return;

    if (this.data) {
      // Обновление категории
      this.authService.updateCategory(this.data.id, this.categoryName).subscribe(() => this.dialogRef.close(true));
    } else {
      // Создание новой категории
      this.authService.addCategory(this.categoryName).subscribe(() => this.dialogRef.close(true));
    }
  }
}
