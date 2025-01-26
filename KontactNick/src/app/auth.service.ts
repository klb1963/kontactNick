import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators'; // ✅ Добавлен map

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object // Проверка среды (SSR/Browser)
  ) {}

  /** ✅ Проверяем, выполняется ли код в браузере */
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** ✅ Сохранение токена в Cookie */
  public saveToken(token: string): void {
    if (this.isBrowser()) {
      document.cookie = `jwt-token=${token}; path=/; Secure; HttpOnly; SameSite=None`;
      console.log('🍪 Token saved in cookie');
    } else {
      console.warn('⚠️ Cannot use document.cookie in SSR mode.');
    }
  }

  /** ✅ Получение токена из Cookie */
  public getToken(): string | null {
    if (this.isBrowser()) {
      const match = document.cookie.match(/jwt-token=([^;]+)/);
      return match ? match[1] : null;
    } else {
      console.warn('⚠️ Cookies are not available in SSR mode.');
      return null;
    }
  }

  /** ✅ Проверка авторизации */
  public isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /** ✅ Метод регистрации */
  public register(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/register`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(response => this.saveToken(response.token)),
      map(() => true),
      catchError(error => {
        console.error('🚨 Registration error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Метод логина */
  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/login`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(response => this.saveToken(response.token)),
      map(() => true), // ✅ Преобразуем в `boolean`, исправляя ошибку типов
      catchError(error => {
        console.error('🚨 Login error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Логаут (очищаем куки) */
  public logout(): void {
    if (this.isBrowser()) {
      console.log('🔴 AuthService: Logging out user');

      this.http.post('http://localhost:8080/api/auth/logout', {}, { withCredentials: true, responseType: 'text' })
        .subscribe({
          next: () => {
            console.log('✅ Logged out successfully');
            document.cookie = 'jwt-token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; Secure; SameSite=None';
            this.router.navigate(['/login']).then(() => {
              window.location.reload();
            });
          },
          error: (err) => console.error('🚨 Logout error:', err)
        });
    }
  }

  /** ✅ Проверка аутентификации */
  public checkAuth(): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/check`, { withCredentials: true }).pipe(
      catchError(error => {
        console.error('🚨 Auth check failed:', error);
        return of(false);
      })
    );
  }

  /** ✅ Получение категорий пользователя */
  public getUserCategories(): Observable<any[]> {
    return this.http.get<any[]>('http://localhost:8080/api/categories/my', { withCredentials: true }).pipe(
      tap(categories => console.log('📂 Categories received:', categories)),
      catchError(error => {
        console.error('🚨 Error fetching categories:', error);
        return of([]);
      })
    );
  }
}
