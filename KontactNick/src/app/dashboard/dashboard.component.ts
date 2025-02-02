import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../auth.service';
import { CategoryDialogComponent } from '../category-dialog/category-dialog.component';
import { FieldDialogComponent } from '../field-dialog/field-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatDialogModule
  ]
})
export class DashboardComponent implements OnInit {
  categories: any[] = [];
  displayedColumns: string[] = ['name', 'description', 'actions'];

  private authService = inject(AuthService);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories() {
    this.authService.getUserCategories().subscribe(categories => {
      this.categories = categories;
    });
  }

  openCategoryDialog(category: any = null) {
    const dialogRef = this.dialog.open(CategoryDialogComponent, {
      width: '400px',
      data: category
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (category) {
          // Редактирование категории
          this.authService.updateCategory(category.id, result).subscribe(() => this.loadCategories());
        } else {
          // Создание новой категории
          this.authService.createCategory(result).subscribe(() => this.loadCategories());
        }
      }
    });
  }

  deleteCategory(id: number) {
    if (confirm('Are you sure you want to delete this category?')) {
      this.authService.deleteCategory(id).subscribe(() => this.loadCategories());
    }
  }

  logout() {
    fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      credentials: 'include'
    })
      .then(response => {
        if (response.ok) {
          console.log("✅ Logged out successfully");
          window.location.href = '/login';
        }
      })
      .catch(error => console.error("❌ Logout failed:", error));
  }

  // Метод для открытия диалога добавления поля
  openFieldDialog(categoryId: number) {
    const dialogRef = this.dialog.open(FieldDialogComponent, {
      width: '400px',
      data: { categoryId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.authService.addFieldToCategory(categoryId, result).subscribe(() => this.loadCategories());
      }
    });
  }
}
