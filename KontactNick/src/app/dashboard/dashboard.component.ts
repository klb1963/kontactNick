import {Component, OnInit, inject, ChangeDetectorRef} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatTableModule} from '@angular/material/table';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {CategoryDialogComponent} from '../category-dialog/category-dialog.component';
import {MatExpansionModule} from '@angular/material/expansion';
import {Router} from '@angular/router';
import {CategoryService} from '../services/category.service';
import {AuthService} from '../services/auth.service';
import {FormsModule} from '@angular/forms';
import {MatListModule} from '@angular/material/list'; // ✅ Исправленный импорт
import {MatButtonModule} from '@angular/material/button';
import {AddContactDialogComponent} from '@app/add-contact/add-contact.component';

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
  selectedCategory: any = null;
  displayedColumns: string[] = ['name', 'description', 'actions'];
  sortedCategories: any[] = [];
  sortOrder: 'asc' | 'desc' = 'asc'; // ✅ По умолчанию A → Z
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
        this.categories = categories.map(category => ({
          ...category,
          fields: category.fields || [] // ✅ Убеждаемся, что всегда массив
        }));
        this.sortCategories();
        console.log("✅ Categories loaded:", this.categories);
      },
      error: (error) => {
        console.error("❌ Error fetching categories:", error);
        this.categories = [];
      }
    });
  }

  /** ✅ Переключение сортировки */
  toggleSortOrder() {
    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    this.sortCategories();
  }

  /** ✅ Метод сортировки */
  sortCategories() {
    this.sortedCategories = [...this.categories].sort((a, b) => {
      const comparison = a.name.localeCompare(b.name);
      return this.sortOrder === 'asc' ? comparison : -comparison;
    });
  }

  /** 👨‍💻 Профайл пользователя */
  loadUserProfile(): void {
    this.authService.getUserProfile().subscribe(
      (profile) => {
        if (profile) {
          // ✅ Убираем префикс "ROLE_"
          if (profile.role && profile.role.startsWith("ROLE_")) {
            profile.role = profile.role.substring(5).toLowerCase();
          }

          this.userProfile = profile;

          // ✅ Логируем профиль
          console.log("✅ User profile loaded:", profile);

          // ✅ Проверяем, есть ли avatarUrl
          if (!profile.avatarUrl) {
            console.warn("⚠️ No avatar URL found in profile:", profile);
          }
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

  // диалоговое окно создания и сохранения категории
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

        action.subscribe(() => {
          this.loadCategories();
        });
      }
    });
  }

// диалоговое окно создания и сохранения контакта в категории
  openAddContactDialog(category: any) {
    console.log("📢 Opening Add Contact Dialog with category:", category);

    if (!category || !category.id) {
      console.error("❌ Ошибка: категория не содержит ID! Полные данные категории:", JSON.stringify(category, null, 2));
      alert("⚠️ Ошибка: Невозможно создать контакт без ID категории!");
      return;
    }

    this.selectedCategory = category;

    const dialogRef = this.dialog.open(AddContactDialogComponent, {
      width: '400px',
      data: { category } // ✅ Передаём только ID категории
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log("✅ Contact data received:", result);

        // ✅ Убираем `googleResourceName`, передаём только `category.id`
        this.categoryService.addContactToCategory(category.id, result).subscribe({
          next: () => {
            console.log("✅ Contact successfully saved in category:", category.name);
            this.loadCategories(); // ✅ Перезагружаем список категорий
          },
          error: (error) => console.error("❌ Error saving contact:", error)
        });
      }
    });
  }

  // удаление категории
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
