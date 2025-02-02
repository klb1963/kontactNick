import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, Observable, of } from 'rxjs';
import { map, tap, switchMap } from 'rxjs/operators';

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

  /** ✅ Получение токена с сервера (если HttpOnly) */
  public getTokenFromServer(): Observable<string | null> {
    return this.http.get<{ token?: string }>(`${this.baseUrl}/token`, {
      withCredentials: true
    }).pipe(
      map(response => response?.token ?? null),
      catchError(error => {
        if (error.status !== 401) {  // ✅ Показываем только важные ошибки
          console.error("🚨 Unexpected error fetching token:", error);
        }
        return of(null);
      })
    );
  }

  /** ✅ Проверка статуса аутентификации */
  public checkAuthStatus(): Observable<boolean> {
    return this.http.get<{ authenticated?: boolean }>(
      `${this.baseUrl}/check`,
      { withCredentials: true }
    ).pipe(
      map((response: any) => response?.authenticated === 'true'),
      catchError(error => {
        if (error.status !== 401) {  // ✅ Логируем только неожиданные ошибки
          console.error('🚨 Unexpected auth check error:', error);
        }
        return of(false);
      })
    );
  }

  /** ✅ Проверка авторизации */
  public isLoggedIn(): Observable<boolean> {
    return this.getTokenFromServer().pipe(
      switchMap(token => token ? this.checkAuthStatus() : of(false)),
      catchError(() => of(false))
    );
  }

  /** ✅ Метод регистрации */
  public register(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/register`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      switchMap(() => this.isLoggedIn()),
      catchError(error => {
        console.error('🚨 Registration error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Логин */
  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/login`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(() => console.log('✅ Login successful, token should be in cookies.')),
      switchMap(() => this.isLoggedIn()),
      catchError(error => {
        console.error('🚨 Login error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Логаут */
  public logout(): void {
    if (this.isBrowser()) {
      console.log('🔴 Logging out user');
      this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true, responseType: 'text' })
        .subscribe({
          next: () => {
            console.log('✅ Logged out successfully');
            this.router.navigate(['/login']).then(() => window.location.reload());
          },
          error: (err) => console.error('🚨 Logout error:', err)
        });
    }
  }

  /** ✅ Получение категорий пользователя (с проверкой токена) */
  public getUserCategories(): Observable<any[]> {
    return this.getTokenFromServer().pipe(
      switchMap(token => {
        if (!token) {
          console.warn("ℹ️ User not authenticated, category request skipped");
          return of([]);
        }
        return this.http.get<any[]>('http://localhost:8080/api/categories', { withCredentials: true }).pipe(
          tap(categories => console.log('📂 Categories received:', categories)),
          catchError(error => {
            console.error('🚨 Error fetching categories:', error);
            return of([]);
          })
        );
      })
    );
  }

  createCategory(category: { name: string, description: string }) {
    return this.http.post('http://localhost:8080/api/categories', category, { withCredentials: true })
      .pipe(
        catchError(error => {
          console.error('🚨 Error creating category:', error);
          return of(null);
        })
      );
  }

  updateCategory(id: number, category: { name: string, description: string  }) {
    return this.http.put(`http://localhost:8080/api/categories/${id}`, category, { withCredentials: true })
      .pipe(
        catchError(error => {
          console.error('🚨 Error updating category:', error);
          return of(null);
        })
      );
  }

  deleteCategory(categoryId: number) {
    return this.http.delete(`http://localhost:8080/api/categories/${categoryId}`, { withCredentials: true });
  }

  addFieldToCategory(categoryId: number, field: { name: string, fieldType: string, value: string }) {
    return this.http.post(`http://localhost:8080/api/categories/${categoryId}/field`, field, { withCredentials: true });
  }

}
