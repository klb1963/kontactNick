import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { first } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true
})
export class DashboardComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.checkAuth()
      .pipe(first())
      .subscribe({
        next: () => {
          console.log('✅ Auth check passed, fetching token...');
          this.fetchToken();
        },
        error: (err) => {
          this.handleCheckAuthError(err);
        }
      });
  }

  private fetchToken(): void {
    const jwtToken = this.getJwtToken(); // ✅ Получаем токен

    if (!jwtToken) {
      console.warn('❌ No JWT found, skipping /api/auth/token request');
      return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);

    this.http.get('/api/auth/token', { headers, withCredentials: true }).subscribe({
      next: (data) => console.log('✅ Token received:', data),
      error: (err) => console.error('❌ Error fetching token:', err)
    });
  }

  logout(): void {
    this.authService.logout();
  }

  private handleCheckAuthError(err: HttpErrorResponse) {
    console.warn('🚨 Dashboard: No valid token found, redirecting to login');
    this.router.navigate(['/login']);
  }

  private getJwtToken(): string | null {
    // ✅ 1. Проверяем localStorage
    let token = localStorage.getItem('jwt-token');

    if (!token) {
      // ✅ 2. Если нет в localStorage, ищем в Cookie
      const cookieToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('jwt-token='));

      if (cookieToken) {
        token = cookieToken.split('=')[1];
        console.log('🍪 JWT найден в Cookie:', token);
      }
    }

    if (token) {
      console.log('🔑 Используем JWT:', token);
    } else {
      console.warn('❌ JWT не найден ни в localStorage, ни в Cookie');
    }

    return token;
  }
}
