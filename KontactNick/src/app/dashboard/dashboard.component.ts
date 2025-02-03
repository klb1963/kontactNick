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
    MatExpansionModule
  ]
})
export class DashboardComponent implements OnInit {
  categories: any[] = [];
  displayedColumns: string[] = ['name', 'description', 'actions'];

  private categoryService = inject(CategoryService);
  private dialog = inject(MatDialog);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories() {
    this.categoryService.getUserCategories().subscribe((categories: any[]) => {
      this.categories = categories;
      this.cdr.detectChanges();
    });
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
