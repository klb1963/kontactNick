import {Injectable, Inject, PLATFORM_ID} from '@angular/core';
import {isPlatformBrowser} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object // Проверка среды (SSR/Browser)
  ) {
  }

  /** ✅ Проверяем, выполняется ли код в браузере */
  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** ✅ Сохранение токена в Cookie (не в localStorage!) */
  saveToken(token: string): void {
    if (this.isBrowser()) {
      document.cookie = `jwt-token=${token}; path=/; Secure; HttpOnly; SameSite=None`;
    } else {
      console.warn('⚠️ Cannot use document.cookie in SSR mode.');
    }
  }

  /** ✅ Получение токена из Cookie */
  getToken(): string | null {
    if (this.isBrowser()) {
      const cookie = document.cookie
        .split('; ')
        .find(row => row.startsWith('jwt-token='));
      return cookie ? cookie.split('=')[1] : null;
    } else {
      console.warn('⚠️ Cookies are not available in SSR mode.');
      return null;
    }
  }

  /** ✅ Проверка авторизации */
  isLoggedIn(): boolean {
    const token = this.getToken();
    console.log('🔍 AuthService: Checking if user is logged in. Token:', token);
    return !!token;
  }

  /** ✅ Метод регистрации (добавлен withCredentials) */
  register(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/register`,
      {email, password},
      {withCredentials: true} // ✅ Передаём куки!
    );
  }

  /** ✅ Метод логина (добавлен withCredentials) */
  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/login`,
      {email, password},
      {withCredentials: true} // ✅ Передаём куки!
    );
  }

  /** ✅ Логаут (очищаем куки) */
  logout(): void {
    if (this.isBrowser()) {
      console.log('🔴 AuthService: Logging out user');

      // ❌ Запрос на сервер для удаления Cookies
      this.http.post('http://localhost:8080/api/auth/logout', {}, { withCredentials: true, responseType: 'text' })
        .subscribe({
          next: (response) => {
            console.log("✅ Server response:", response); // Теперь не будет ошибки парсинга

            // ❌ Удаляем локально
            localStorage.removeItem('authToken');
            document.cookie = "jwt-token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; Secure; SameSite=None";

            // 🔄 **Перенаправляем через Angular Router**
            this.router.navigate(['/login']).then(() => {
              window.location.reload(); // 💡 Гарантированная перезагрузка UI
            });
          },
          error: (err) => {
            console.error('🚨 Logout error:', err);
          }
        });
    }
  }

  /** ✅ Проверка аутентификации (добавлен withCredentials) */
  checkAuth()
    :
    Observable<boolean> {
    return this.http.get<boolean>(
      `${this.baseUrl}/check`,
      {withCredentials: true} // ✅ Передаём куки!
    );
  }
}
