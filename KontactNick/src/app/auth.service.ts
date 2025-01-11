import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})

export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  register(email: string, password: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, { email, password });
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, { email, password });
  }

  saveToken(token: string): void {
    localStorage.setItem('authToken', token);
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  // это от аутентификации через Google
  // getTokenFromServer(): Observable<string> {
  //   return this.http.get<string>(`${this.baseUrl}/api/token`);
  // }
  //
  // saveToken(token: string): void {
  //   console.log('Token to save:', token); // Для отладки
  //   localStorage.setItem('authToken', token);
  // }

  isLoggedIn(): boolean {
    const token = localStorage.getItem('authToken');
    return !!token; // Проверяем, есть ли токен
  }
}
