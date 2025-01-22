import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth'; // ✅ Добавляем базовый URL API

  constructor(private http: HttpClient, private router: Router) {
  }

  /** ✅ Метод регистрации */
  register(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/register`, {email, password});
  }

  /** ✅ Метод логина */
  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/login`, {email, password});
  }

  /** ✅ Сохранение токена */
  saveToken(token: string): void {
    console.log('✅ AuthService: Saving token:', token);
    if (token) {
      localStorage.setItem('authToken', token);
    } else {
      console.warn('⚠️ No token received, not saving.');
    }
  }

  /** ✅ Получение токена */
  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  /** ✅ Проверка авторизации */
  isLoggedIn(): boolean {
    const token = this.getToken();
    console.log('🔍 AuthService: Checking if user is logged in. Token:', token);
    return !!token;
  }

  /** ✅ Получение токена при авторизации через Google от эндпойнта на бэкенде */
  getGoogleToken(): Observable<any> {
    console.log('🔍 AuthService: Requesting token from backend...');

    const headers = {Authorization: `Bearer ${this.getToken()}`};
    return this.http.get<{ token: string }>('http://localhost:8080/api/auth/token', {headers});
  }

  /** ✅ Получение токена из URL */
  private getTokenFromUrl(): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('token');
  }

  /** ✅ Обработка входа через Google */
  handleGoogleLogin(): void {
    console.log('🔄 AuthService: Handling Google login...');

    this.getGoogleToken().subscribe({
      next: (response) => {
        console.log('✅ AuthService: Google token received:', response.token);
        this.saveToken(response.token);
        this.router.navigate(['/dashboard']); // ✅ Перенаправляем, если токен найден
      },
      error: (err) => {
        console.error('🚨 AuthService: Error fetching Google token:', err);
        this.router.navigate(['/login']); // ❌ Если ошибка — отправляем на login
      }
    });
  }

  /** ✅ Очистка параметров URL */
  private clearQueryParams(): void {
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  /** ✅ Логаут */
  logout(): void {
    console.log('🔴 AuthService: Logging out user');
    localStorage.removeItem('authToken'); // ✅ Удаляем токен
    this.router.navigate(['/login']);
  }

  checkAuth(): Observable<boolean> {
    return this.http.get<boolean>('http://localhost:8080/api/auth/check', {withCredentials: true});
  }
}
