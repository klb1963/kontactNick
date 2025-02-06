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
    console.log('📡 Sending GET request to /api/auth/token...');
    return this.http.get<{ token?: string }>(`${this.baseUrl}/token`, {
      withCredentials: true
    }).pipe(
      tap(response => console.log("🔑 Raw response from server:", response)),
      map(response => response?.token ?? null),
      tap(token => console.log("🔑 Extracted Token:", token)),
      catchError(error => {
        console.error("🚨 Error fetching token from server:", error);
        return of(null);
      })
    );
  }

  /** ✅ Проверка статуса аутентификации */
  public checkAuthStatus(): Observable<boolean> {
    console.log('📡 Sending auth check request...');
    return this.http.get<{ authenticated?: boolean }>(
      `${this.baseUrl}/check`,
      { withCredentials: true }
    ).pipe(
      tap(response => console.log('🔍 Auth check response:', response)),
      map((response: any) => response?.authenticated === 'true'),
      catchError(error => {
        console.error('🚨 Auth check failed:', error);
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
          console.warn('❌ No JWT found, skipping category request');
          return of([]);
        }
        return this.http.get<any[]>(`${this.baseUrl}/categories/my`, { withCredentials: true }).pipe(
          tap(categories => console.log('📂 Categories received:', categories)),
          catchError(error => {
            console.error('🚨 Error fetching categories:', error);
            return of([]);
          })
        );
      })
    );
  }
}
