import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, tap, switchMap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

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

  private jwtAccessToken: string | null = null; // Локальное хранилище токена

  /** ✅ Получение валидного JWT Access Token */
  getValidJwtAccessToken(): Observable<string | null> {
    // Если токен уже есть в кэше, используем его
    if (this.jwtAccessToken) {
      console.log("🔄 Используем кэшированный JWT Access Token:", this.jwtAccessToken);
      return of(this.jwtAccessToken);
    }

    // Запрашиваем токен с сервера
    return this.getTokenFromServer().pipe(
      tap((jwtAccessToken: string | null) => {
        if (jwtAccessToken) {
          console.log("✅ JWT Access Token получен:", jwtAccessToken);
          this.jwtAccessToken = jwtAccessToken; // Кэшируем токен
        } else {
          console.warn("⚠️ JWT Access Token отсутствует!");
          this.jwtAccessToken = null;
        }
      }),
      catchError(error => {
        console.error("❌ Ошибка при получении JWT Access Token:", error);
        this.jwtAccessToken = null; // Сбрасываем кэш при ошибке
        return of(null);
      })
    );
  }

  /** ✅ Запрос нового JWT Access Token с сервера */
  getTokenFromServer(): Observable<string | null> {
    return this.http.get<{ token?: string }>(`${this.baseUrl}/auth/token`, { withCredentials: true }).pipe(
      map(response => response?.token ?? null),
      tap(token => {
        if (token) {
          console.log("✅ Новый JWT Access Token получен с сервера:", token);
        } else {
          console.warn("⚠️ Сервер не вернул токен.");
        }
      }),
      catchError(error => {
        console.error("❌ Ошибка при запросе JWT Access Token с сервера:", error);
        return of(null);
      })
    );
  }

  /** ✅ Принудительное обновление токена */
  refreshJwtAccessToken(): Observable<string | null> {
    console.log("🔄 Принудительное обновление JWT Access Token...");
    this.jwtAccessToken = null; // Сбрасываем кэш
    return this.getValidJwtAccessToken();
  }

  /** ✅ Проверка статуса входа */
  isLoggedIn(): Observable<boolean> {
    return this.getValidJwtAccessToken().pipe(
      map(token => !!token),
      catchError(() => of(false))
    );
  }

  /** ✅ Декодирование email из токена */
  getCurrentUserEmail(): Observable<string | null> {
    return this.getValidJwtAccessToken().pipe(
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

  /** ✅ Получение Google Access Token с сервера */
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
