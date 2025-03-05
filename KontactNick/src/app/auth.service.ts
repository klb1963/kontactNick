import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, tap, switchMap, catchError } from 'rxjs/operators';

// 🔹 Что изменилось?
// 1.	Убраны лишние методы (getUserCategories(), register() – они не относятся к аутентификации).
// 2.	Исправлена checkAuthStatus() → теперь корректно работает с boolean.
// 3.	getGoogleAccessToken() не записывает в localStorage – этим должна заниматься компонента, если нужно.
// 4.	Упрощена логика isLoggedIn() – без лишних if.
// 5.	Чистый код с лаконичными catchError() – нет ненужных вложенностей.

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  /** ✅ Проверяем, выполняется ли код в браузере */
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** ✅ Получение JWT-токена с сервера */
  public getTokenFromServer(): Observable<string | null> {
    console.log('📡 Fetching JWT token...');
    return this.http.get<{ token: string }>(`${this.baseUrl}/token`, { withCredentials: true }).pipe(
      tap(response => console.log("🔑 JWT Token Response:", response)),
      map(response => response?.token || null),
      catchError(error => {
        console.error("🚨 Ошибка при получении JWT-токена:", error);
        return of(null);
      })
    );
  }

  /** ✅ Проверка аутентификации (JWT) */
  public checkAuthStatus(): Observable<boolean> {
    console.log('📡 Проверяем статус аутентификации...');

    return this.http.get<{ authenticated?: boolean | string }>(
      `${this.baseUrl}/auth/check`, { withCredentials: true }
    ).pipe(
      tap(response => console.log("🔍 Auth check full response:", response)),  // Логируем весь ответ
      map(response => response?.authenticated === true || response?.authenticated === "true"), // Фикс
      catchError(error => {
        console.error('🚨 Auth check failed:', error);
        return of(false);
      })
    );
  }

  /** ✅ Проверка, залогинен ли пользователь */
  public isLoggedIn(): Observable<boolean> {
    return this.getTokenFromServer().pipe(
      switchMap(token => token ? this.checkAuthStatus() : of(false)),
      catchError(() => of(false))
    );
  }

  /** ✅ Логин */
  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/login`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(() => console.log('✅ Успешный вход, токен должен быть в cookies')),
      switchMap(() => this.isLoggedIn()),
      catchError(error => {
        console.error('🚨 Ошибка входа:', error);
        return of(false);
      })
    );
  }

  /** ✅ Логаут */
  public logout(): void {
    if (!this.isBrowser()) return;

    console.log('🔴 Выход пользователя...');
    this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true, responseType: 'text' })
      .subscribe({
        next: () => {
          console.log('✅ Успешный выход');
          localStorage.removeItem('google_access_token');
          this.router.navigate(['/login']).then(() => window.location.reload());
        },
        error: (err) => console.error('🚨 Ошибка выхода:', err)
      });
  }

  /** ✅ Получение Google Access Token с сервера */
  getGoogleAccessToken(): Observable<string | null> {
    return this.http.get<{ google_access_token: string }>(`${this.baseUrl}/google-token`, {
      withCredentials: true
    }).pipe(
      tap(response => console.log("🔄 Ответ от сервера при получении токена:", response)),
      map(response => response?.google_access_token || null),
      catchError(error => {
        console.error("❌ Ошибка при получении Google Access Token!", error);
        return of(null);
      })
    );
  }
}
