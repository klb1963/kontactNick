import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { first, switchMap, of } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true,
  imports: [CommonModule]
})
export class DashboardComponent implements OnInit {
  categories: any[] = [];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('🔍 DashboardComponent initialized, checking authentication...');

    this.authService.checkAuthStatus().pipe(
      first(),
      switchMap(isAuthenticated => {
        if (!isAuthenticated) {
          console.warn('❌ User is NOT authenticated, redirecting to login');
          this.router.navigate(['/login']);
          return of([]); // ✅ Возвращаем пустой массив, чтобы не ломался `subscribe`
        }

        console.log('✅ User is authenticated, retrieving JWT...');
        return this.authService.getTokenFromServer().pipe(
          switchMap(token => {
            if (!token) {
              console.warn('❌ No JWT found, skipping category request');
              return of([]);
            }

            console.log('📡 Fetching categories with JWT...');
            return this.authService.getUserCategories();
          })
        );
      })
    ).subscribe({
      next: (categories: any[]) => {  // Добавляем тип `any[]`
        this.categories = categories;
        console.log('✅ Categories loaded:', categories);
      },
      error: (err: any) => console.error('🚨 Error loading categories:', err)
    });
  }

  logout(): void {
    console.log('🔴 Logging out...');
    this.authService.logout();
  }
}
