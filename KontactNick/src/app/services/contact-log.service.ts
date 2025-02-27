import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, switchMap, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

// 🔥 Исправления и улучшения:
//
// ✅ Используем AuthService для автоматического получения токена вместо localStorage.getItem('token').
// ✅ Правильный baseUrl из environment.ts.
// ✅ Более безопасная передача токена через withCredentials: true.
// ✅ Кастомный лог перед отправкой логов, чтобы видеть, какие данные уходят.

@Injectable({
  providedIn: 'root'
})
export class ContactLogService {
  private apiUrl = `${environment.apiBaseUrl}/contact-log`; // ✅ Эндпоинт для логов

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** ✅ Запись лога при добавлении контакта */
  logContactAddition(logData: any): Observable<any> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(token => {
        if (!token) {
          console.error("❌ Ошибка: нет Google Access Token, логирование невозможно!");
          return of(null);
        }

        const httpOptions = {
          headers: new HttpHeaders({
            'Authorization': `Bearer ${token}`, // ✅ Передаем Google Access Token
            'Content-Type': 'application/json'
          }),
          withCredentials: true
        };

        console.log("📌 Отправка логов в Contact Log API:", logData);
        return this.http.post(`${this.apiUrl}/add`, logData, httpOptions);
      })
    );
  }

  /** ✅ Получение всех логов */
  getLogs(): Observable<any[]> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(token => {
        if (!token) {
          console.error("❌ Ошибка: нет Google Access Token, невозможно получить логи!");
          return of([]);
        }

        const httpOptions = {
          headers: new HttpHeaders({
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }),
          withCredentials: true
        };

        console.log("📡 Запрос логов из Contact Log API...");
        return this.http.get<any[]>(`${this.apiUrl}/all`, httpOptions);
      })
    );
  }
}
