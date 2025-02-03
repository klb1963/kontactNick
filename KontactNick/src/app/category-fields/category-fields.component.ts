import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { MatDialog } from '@angular/material/dialog';
import { FieldDialogComponent } from '../field-dialog/field-dialog.component';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-category-fields',
  standalone: true,
  templateUrl: './category-fields.component.html',
  styleUrls: ['./category-fields.component.css'],
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule]
})
export class CategoryFieldsComponent implements OnInit {
  categoryId!: number;
  fields: any[] = [];
  categoryName: string = ''; // ✅ Для хранения имени категории

  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.categoryId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadCategory(); // ✅ Загружаем информацию о категории
    this.loadFields();
  }

  loadCategory(): void {
    this.authService.getCategoryById(this.categoryId).subscribe(category => {
      this.categoryName = category.name; // ✅ Устанавливаем имя категории
    });
  }

  loadFields(): void {
    this.authService.getCategoryFields(this.categoryId).subscribe(fields => {
      console.log("📥 Refreshed fields:", fields); // Лог обновленных полей
      this.fields = fields;
    });
  }

  // ✅ Добавить поле в категорию
  addField(): void {
    const dialogRef = this.dialog.open(FieldDialogComponent, {
      width: '400px',
      data: { categoryId: this.categoryId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.authService.addFieldToCategory(this.categoryId, result).subscribe({
          next: () => {
            console.log("✅ Field added, refreshing fields...");
            this.loadFields(); // 🔄 Загружаем обновленные данные
          },
          error: (err) => console.error("❌ Error adding field:", err)
        });
      }
    });
  }

  // ✅ Редактировать поле в категории
  editField(field: any, categoryId: number): void {
    const dialogRef = this.dialog.open(FieldDialogComponent, {
      width: '400px',
      data: {
        id: field.id,
        name: field.name,
        fieldType: field.fieldType,
        value: field.value
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.authService.updateField(categoryId, field.id, result)
          .subscribe(() => this.loadFields());
      }
    });
  }

  // ✅ Удалить поле из категории
  deleteField(fieldId: number): void {
    if (confirm('Are you sure you want to delete this field?')) {
      this.authService.deleteField(this.categoryId, fieldId).subscribe({
        next: () => {
          console.log(`✅ Field with ID ${fieldId} deleted.`);
          this.loadFields();  // Обновление списка полей
        },
        error: (err) => console.error("❌ Error deleting field:", err)
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
