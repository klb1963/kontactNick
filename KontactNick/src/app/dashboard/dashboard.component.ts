import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { first } from 'rxjs';
import { CommonModule } from '@angular/common'; // ✅ Добавляем CommonModule

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true,
  imports: [CommonModule] // ✅ Добавляем в imports
})
export class DashboardComponent implements OnInit {
  categories: any[] = []; // ✅ Массив для хранения категорий

  constructor(
    private authService: AuthService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('🔍 DashboardComponent initialized, checking authentication...');
    this.authService.checkAuth()
      .pipe(first())
      .subscribe({
        next: (isAuthenticated: boolean) => {
          if (isAuthenticated) {
            console.log('✅ User is authenticated, loading categories...');
            this.loadCategories();
          } else {
            console.warn('❌ User is NOT authenticated, redirecting to login');
            this.router.navigate(['/login']);
          }
        },
        error: () => {
          console.warn('🚨 Authentication check failed, redirecting to login');
          this.router.navigate(['/login']);
        }
      });
  }

  private loadCategories(): void {
    const jwtToken = this.authService.getToken();
    if (!jwtToken) {
      console.warn('❌ No JWT found, skipping category request');
      return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);

    this.http.get<any[]>('http://localhost:8080/api/categories/my', { headers, withCredentials: true })
      .subscribe({
        next: (data) => {
          console.log('✅ Categories received:', data);
          this.categories = data;
        },
        error: (err: any) => console.error('❌ Error fetching categories:', err)
      });
  }

  logout(): void {
    console.log('🔴 Logging out...');
    this.authService.logout();
  }
}
