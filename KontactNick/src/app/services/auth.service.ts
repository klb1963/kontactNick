import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { map, tap, switchMap, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api'; // ✅ Используем только baseUrl

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  getTokenFromServer(): Observable<string | null> {
    return this.http.get<{ token?: string }>(`${this.baseUrl}/auth/token`, {
      withCredentials: true
    }).pipe(
      map(response => response?.token ?? null),
      catchError(error => {
        if (error.status !== 401) {
          console.error("🚨 Unexpected error fetching token:", error);
        }
        return of(null);
      })
    );
  }

  isLoggedIn(): Observable<boolean> {
    return this.getTokenFromServer().pipe(
      map(token => !!token),
      catchError(() => of(false))
    );
  }

  getCurrentUserEmail(): Observable<string | null> {
    return this.getTokenFromServer().pipe(
      map(token => {
        if (token) {
          try {
            const payloadBase64 = token.split('.')[1];
            if (!payloadBase64) throw new Error('Invalid token format');

            const payload = JSON.parse(atob(payloadBase64));
            return payload.email || payload.sub || null;
          } catch (error) {
            console.error('🚨 Error decoding token:', error);
            return null;
          }
        }
        return null;
      }),
      catchError(error => {
        console.error('🚨 Error fetching user email:', error);
        return of(null);
      })
    );
  }

  /** ✅ Проверка статуса аутентификации */
  public checkAuthStatus(): Observable<boolean> {
    return this.http.get<{ authenticated?: boolean }>(
      `${this.baseUrl}/auth/check`,
      { withCredentials: true }
    ).pipe(
      map((response: any) => response?.authenticated === 'true'),
      catchError((error: any) => {
        if (error.status === 401 && this.isBrowser()) {
          this.router.navigate(['/login']);
        }
        console.error('🚨 Unexpected auth check error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Логин */
  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/auth/login`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(() => console.log('✅ Login successful, token should be in cookies.')),
      switchMap(() => this.checkAuthStatus()),
      catchError((error: any) => {
        console.error('🚨 Login error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Регистрация */
  public register(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/auth/register`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      switchMap(() => this.checkAuthStatus()),
      catchError((error: any) => {
        console.error('🚨 Registration error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Получение user profile */
  getUserProfile(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profile`, { withCredentials: true }).pipe(
      tap(profile => console.log("✅ User profile loaded:", profile)),
      catchError(error => {
        console.error("❌ Error fetching user profile:", error);
        return of(null); // ✅ Возвращаем null, если ошибка
      })
    );
  }

  /** ✅ Обновление ника */
  updateNick(newNick: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/profile/nick`, { nick: newNick }, { withCredentials: true });
  }
}
