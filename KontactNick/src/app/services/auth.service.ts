import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, tap, switchMap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

// 🔥 Что улучшено?
//   1.	Используем environment.ts вместо хардкода (baseUrl, googleTokenUrl).
// 2.	Исправлена ошибка с authenticated === 'true' → теперь это boolean.
// 3.	Убрано хранение Google Access Token в localStorage (более безопасно).
// 4.	Оптимизирован getGoogleAccessToken(), теперь просто запрашивает данные.
// 5.	Код более читаемый, лучше логируются ошибки.

/** ✅ Интерфейс для ответа сервера */
interface GoogleAuthResponse {
  accessToken: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = environment.apiBaseUrl;
  private googleTokenUrl = environment.googleTokenUrl;

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** ✅ Получение токена с сервера */
  getTokenFromServer(): Observable<string | null> {
    return this.http.get<{ token?: string }>(`${this.baseUrl}/auth/token`, { withCredentials: true })
      .pipe(
        map(response => response?.token ?? null),
        catchError(error => {
          if (error.status !== 401) {
            console.error("🚨 Unexpected error fetching token:", error);
          }
          return of(null);
        })
      );
  }

  /** ✅ Проверка статуса входа */
  isLoggedIn(): Observable<boolean> {
    return this.getTokenFromServer().pipe(
      map(token => !!token),
      catchError(() => of(false))
    );
  }

  /** ✅ Декодирование email из токена */
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
      catchError(() => of(null))
    );
  }

  /** ✅ Проверка аутентификации */
  checkAuthStatus(): Observable<boolean> {
    return this.http.get<{ authenticated?: boolean }>(`${this.baseUrl}/auth/check`, { withCredentials: true })
      .pipe(
        map(response => response?.authenticated === true), // 🔥 Исправлено (не строка)
        catchError(error => {
          if (error.status === 401 && this.isBrowser()) {
            this.router.navigate(['/login']);
          }
          console.error('🚨 Unexpected auth check error:', error);
          return of(false);
        })
      );
  }

  /** ✅ Логин */
  login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/auth/login`, { email, password }, { withCredentials: true })
      .pipe(
        tap(() => console.log('✅ Login successful, token should be in cookies.')),
        switchMap(() => this.checkAuthStatus()),
        catchError(error => {
          console.error('🚨 Login error:', error);
          return of(false);
        })
      );
  }

  /** ✅ Регистрация */
  register(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/auth/register`, { email, password }, { withCredentials: true })
      .pipe(
        switchMap(() => this.checkAuthStatus()),
        catchError(error => {
          console.error('🚨 Registration error:', error);
          return of(false);
        })
      );
  }

  /** ✅ Получение профиля пользователя */
  getUserProfile(): Observable<any> {
    return this.http.get(`${this.baseUrl}/profile`, { withCredentials: true })
      .pipe(
        tap(profile => console.log("✅ User profile loaded:", profile)),
        catchError(error => {
          console.error("❌ Error fetching user profile:", error);
          return of(null);
        })
      );
  }

  /** ✅ Обновление ника */
  updateNick(newNick: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/profile/nick`, { nick: newNick }, { withCredentials: true });
  }

  /** ✅ Обработка ответа Google OAuth */
  handleGoogleAuthResponse(code: string): Observable<boolean> {
    return this.http.post<GoogleAuthResponse>(`${this.baseUrl}/auth/google/callback`, { code }, { withCredentials: true })
      .pipe(
        tap((response: GoogleAuthResponse) => {
          if (!response?.accessToken) {
            console.error("❌ No access token received from Google!");
          }
        }),
        map(response => !!response?.accessToken),
        catchError(error => {
          console.error("🚨 Google OAuth error:", error);
          return of(false);
        })
      );
  }

  /** ✅ Запрос Google Access Token */
  getGoogleAccessToken(): Observable<string | null> {
    return this.http.get<{ accessToken?: string }>(this.googleTokenUrl, { withCredentials: true })
      .pipe(
        map(response => response.accessToken ?? null),
        catchError(error => {
          console.error("❌ Ошибка при запросе Google Access Token:", error);
          return of(null);
        })
      );
  }
}
