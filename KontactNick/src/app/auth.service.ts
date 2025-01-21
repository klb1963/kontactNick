import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth'; // ✅ Добавляем базовый URL API

  constructor(private http: HttpClient, private router: Router) {}

  /** ✅ Метод регистрации */
  register(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/register`, { email, password });
  }

  /** ✅ Метод логина */
  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.baseUrl}/login`, { email, password });
  }

  /** ✅ Сохранение токена */
  saveToken(token: string): void {
    if (token) {
      console.log('✅ AuthService: Saving token:', token);
      localStorage.setItem('authToken', token);
    } else {
      console.warn('⚠️ AuthService: No token received, not saving.');
    }
  }

  /** ✅ Получение токена */
  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  /** ✅ Проверка авторизации */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /** ✅ Получение токена из URL */
  private getTokenFromUrl(): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('token');
  }

  /** ✅ Обработка входа через Google */
  handleGoogleLogin(): void {
    const token = this.getTokenFromUrl();
    if (token) {
      console.log('✅ AuthService: Google token found in URL:', token);
      this.saveToken(token);
      this.clearQueryParams(); // ✅ Удаляем токен из URL после сохранения
      this.router.navigate(['/dashboard']); // ✅ Перенаправляем, если токен найден
    } else {
      console.warn('⚠️ AuthService: No Google token found in URL.');
    }
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
}
