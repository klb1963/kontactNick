import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { FieldDialogComponent } from '../field-dialog/field-dialog.component';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CategoryService } from '../services/category.service';
import { AddContactDialogComponent } from '../add-contact/add-contact.component';

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
  categoryName: string = '';

  private authService = inject(AuthService);
  private categoryService = inject(CategoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.authService.isLoggedIn().subscribe(isAuth => {
      if (isAuth) {
        this.categoryId = Number(this.route.snapshot.paramMap.get('id'));
        this.loadCategory();
        this.loadFields();
      } else {
        console.warn("❌ Пользователь не аутентифицирован. Перенаправление на страницу входа.");
        this.router.navigate(['/login']);  // ✅ Перенаправление на страницу входа
      }
    });
  }

  loadCategory(): void {
    this.categoryService.getCategoryById(this.categoryId).subscribe({
      next: (category) => {
        this.categoryName = category.name;
        console.log("✅ Category loaded:", category);
      },
      error: (err) => {
        console.error("❌ Error loading category:", err);
        if (err.status === 404) {
          alert('Категория не найдена. Возможно, она была удалена.');
          this.router.navigate(['/dashboard']);
        }
      }
    });
  }

  loadFields(): void {
    this.categoryService.getCategoryFields(this.categoryId).subscribe({
      next: (fields) => {
        console.log("📥 Refreshed fields:", fields);
        this.fields = fields;
      },
      error: (err) => {
        console.error("❌ Error loading fields:", err);
        if (err.status === 404) {
          alert('Поля не найдены. Возможно, категория была удалена.');
          this.router.navigate(['/dashboard']);
        }
      }
    });
  }

  addField(): void {
    const dialogRef = this.dialog.open(FieldDialogComponent, {
      width: '400px',
      data: { categoryId: this.categoryId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.categoryService.addFieldToCategory(this.categoryId, result).subscribe({
          next: () => {
            console.log("✅ Field added, refreshing fields...");
            this.loadFields();
          },
          error: (err) => console.error("❌ Error adding field:", err)
        });
      }
    });
  }

  editField(field: any, categoryId: number): void {
    if (!field || !field.id) {
      console.warn('⚠️ Поле не найдено для редактирования.');
      return;
    }

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
        this.categoryService.updateField(categoryId, field.id, result)
          .subscribe({
            next: () => this.loadFields(),
            error: (error) => {
              console.error('❌ Ошибка при обновлении поля:', error);
              if (error.status === 404) {
                alert('Поле не найдено. Возможно, оно было удалено.');
                this.loadFields();
              }
            }
          });
      }
    });
  }

  deleteField(fieldId: number): void {
    if (confirm('Вы уверены, что хотите удалить это поле?')) {
      this.categoryService.deleteField(this.categoryId, fieldId).subscribe({
        next: () => {
          console.log(`✅ Field with ID ${fieldId} deleted.`);
          this.loadFields();
        },
        error: (err) => {
          console.error("❌ Error deleting field:", err);
          if (err.status === 404) {
            alert('Поле или категория не найдены. Возможно, они были удалены.');
            this.router.navigate(['/dashboard']);
          }
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  addContact(categoryId: number): void {
    const dialogRef = this.dialog.open(AddContactDialogComponent, {
      width: '400px',
      data: { categoryId: categoryId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('✅ Contact added:', result);
      }
    });
  }

}
