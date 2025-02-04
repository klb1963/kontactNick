import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CategoryDialogComponent } from '../category-dialog/category-dialog.component';
import { MatExpansionModule } from '@angular/material/expansion';
import { Router } from '@angular/router';
import { CategoryService } from '../services/category.service';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms';
import { MatListModule } from '@angular/material/list'; // ✅ Исправленный импорт
import { MatButtonModule } from '@angular/material/button';

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
    MatDialogModule,
    MatExpansionModule,
    FormsModule,
    MatListModule, // ✅ Подключение MatListModule
    MatButtonModule
  ]
})
export class DashboardComponent implements OnInit {
  categories: any[] = []; // ✅ Инициализация массива
  displayedColumns: string[] = ['name', 'description', 'actions'];
  userProfile: any;

  isEditingNick = false;
  editableNick = '';
  nickError = '';

  private categoryService = inject(CategoryService);
  private dialog = inject(MatDialog);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.isLoggedIn().subscribe(isAuth => {
      if (isAuth) {
        this.loadCategories();
        this.loadUserProfile();
      } else {
        console.warn("❌ Пользователь не аутентифицирован. Запросы к API не выполняем.");
      }
    });
  }

  loadCategories() {
    this.categoryService.getUserCategories().subscribe({
      next: (categories: any[]) => {
        this.categories = categories || []; // ✅ Предотвращение undefined
      },
      error: (error) => {
        console.error("❌ Error fetching categories:", error);
        this.categories = []; // ✅ Установка пустого массива при ошибке
      }
    });
  }

  loadUserProfile(): void {
    this.authService.getUserProfile().subscribe(
      (profile) => {
        if (profile) {
          this.userProfile = profile;
          console.log("✅ User profile loaded:", profile);
        } else {
          console.warn("⚠️ Профиль не загружен");
        }
      },
      (error) => {
        console.error("❌ Error fetching user profile:", error);
      }
    );
  }

  editNick(): void {
    this.isEditingNick = true;
    this.editableNick = this.userProfile.nick;
    this.nickError = '';
  }

  saveNick(): void {
    if (this.editableNick.trim() === '') {
      this.nickError = 'Nick cannot be empty.';
      return;
    }

    this.authService.updateNick(this.editableNick).subscribe({
      next: (response: any) => {
        this.userProfile.nick = this.editableNick;
        this.isEditingNick = false;
        this.nickError = '';
      },
      error: (error) => {
        if (error.status === 400 && error.error.message === 'Nick already exists') {
          this.nickError = 'This nick is already taken. Please choose another.';
        } else {
          this.nickError = 'An unexpected error occurred.';
        }
      }
    });
  }

  cancelEdit(): void {
    this.isEditingNick = false;
    this.editableNick = this.userProfile.nick;
    this.nickError = '';
  }

  openCategoryDialog(category: any = null) {
    const dialogRef = this.dialog.open(CategoryDialogComponent, {
      width: '400px',
      data: category
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const action = category
          ? this.categoryService.updateCategory(category.id, result)
          : this.categoryService.createCategory(result);

        action.subscribe(() => this.loadCategories());
      }
    });
  }

  deleteCategory(id: number) {
    if (confirm('Are you sure you want to delete this category?')) {
      this.categoryService.deleteCategory(id).subscribe(() => this.loadCategories());
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

  viewCategoryFields(categoryId: number) {
    this.router.navigate(['/category', categoryId]);
  }
}
