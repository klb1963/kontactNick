import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../auth.service';
import { CategoryDialogComponent } from '../category-dialog/category-dialog.component'; // ✅ Добавлен импорт

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
    // CategoryDialogComponent // ✅ Добавляем компонент в `imports`
  ]
})
export class DashboardComponent implements OnInit {
  categories: any[] = [];
  displayedColumns: string[] = ['name', 'actions'];

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
    const dialogRef = this.dialog.open(CategoryDialogComponent, { // ✅ Теперь `CategoryDialogComponent` доступен
      width: '400px',
      data: category
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadCategories();
      }
    });
  }

  deleteCategory(id: number) {
    if (confirm('Are you sure you want to delete this category?')) {
      this.authService.deleteCategory(id).subscribe(() => this.loadCategories());
    }
  }
}
