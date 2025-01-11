import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getTokenFromServer(): Observable<string> {
    return this.http.get<string>(`${this.baseUrl}/api/token`);
  }

  saveToken(token: string): void {
    console.log('Token to save:', token); // Для отладки
    localStorage.setItem('authToken', token);
  }

  isLoggedIn(): boolean {
    const token = localStorage.getItem('authToken');
    return !!token; // Проверяем, есть ли токен
  }
}
