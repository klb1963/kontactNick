import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {catchError, Observable, of, shareReplay} from 'rxjs';
import { map, tap, switchMap } from 'rxjs/operators';
import {environment} from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';
  private googleBaseUrl = 'http://localhost:8080/api/google';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  /** ✅ Проверяем, выполняется ли код в браузере */
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** ✅ Получение **JWT-токена** с сервера */
  public getTokenFromServer(): Observable<string | null> {
    console.log('📡 Fetching JWT token...');
    return this.http.get<{ token?: string }>(`${this.baseUrl}/token`, {
      withCredentials: true
    }).pipe(
      tap(response => console.log("🔑 JWT Token Response:", response)),
      map(response => response?.token ?? null),
      catchError(error => {
        console.error("🚨 Error fetching JWT token:", error);
        return of(null);
      })
    );
  }

  /** ✅ Проверка статуса аутентификации (JWT) */
  public checkAuthStatus(): Observable<boolean> {
    console.log('📡 Checking auth status...');

    return this.http.get<{ authenticated: boolean }>(`${this.baseUrl}/auth/check`, {
      withCredentials: true // ✅ Обязательно, чтобы браузер отправлял куки
    }).pipe(
      tap(response => console.log('🔍 Auth check response:', response)),

      map((response: { authenticated: boolean }) => { // ✅ Явно указываем тип
        const isAuthenticated = response.authenticated === 'true'; // ✅ Проверяем булево
        console.log("🔑 User is authenticated:", isAuthenticated);
        return isAuthenticated;
      }),

      catchError(error => {
        console.error('🚨 Auth check failed:', error);
        return of(false); // ✅ Если ошибка — возвращаем false
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
            localStorage.removeItem('googleAccessToken');  // ❌ Удаляем токен при логауте
            this.router.navigate(['/login']).then(() => window.location.reload());
          },
          error: (err) => console.error('🚨 Logout error:', err)
        });
    }
  }

  /** ✅ Получение категорий пользователя */
  public getUserCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories/my`, { withCredentials: true }).pipe(
      tap(categories => console.log('📂 Categories received:', categories)),
      catchError(error => {
        console.error('🚨 Error fetching categories:', error);
        return of([]);
      })
    );
  }

  /** ✅ Получение **Google Access Token** из `localStorage`, если мы в браузере */
  public getAccessToken(): string | null {
    if (typeof window !== 'undefined') { // Проверка, что код выполняется в браузере
      return localStorage.getItem('googleAccessToken');
    }
    return null;
  }

  /**
   * ✅ Получение Google Access Token с сервера
   * */
  getGoogleAccessToken(): Observable<string | null> {
    const url: string = `${environment.apiBaseUrl}/auth/google-token`; // Используем apiBaseUrl

    return this.http.get<{ google_access_token?: string }>(url, {
      withCredentials: true  // ⚠️ Передаём JWT/куки аутентификации
    }).pipe(
      tap((response: { google_access_token?: string }) => {
        console.log("🔄 Ответ от сервера при получении токена:", response);

        if (!response.google_access_token) {
          console.error("❌ Ошибка: сервер не вернул google_access_token!", response);
          return;
        }

        console.log("✅ Новый Google Access Token:", response.google_access_token);
        localStorage.setItem("google_access_token", response.google_access_token);
      }),
      map((response: { google_access_token?: string }) => response.google_access_token || null),
      catchError(err => {
        console.error("❌ Ошибка при получении Google Access Token!", err);
        return of(null);
      })
    );
  }

}
