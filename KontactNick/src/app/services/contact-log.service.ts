import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ContactLogService {
  private apiUrl = 'http://localhost:8080/api/contact-log'; // ✅ Эндпоинт для логов

  constructor(private http: HttpClient) {}

  // ✅ Запись лога при добавлении контакта
  logContactAddition(logData: any): Observable<any> {
    const token = localStorage.getItem('token'); // ✅ Получаем токен
    const httpOptions = {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`, // ✅ Передаем JWT-токен
        'Content-Type': 'application/json'
      }),
      withCredentials: true // ✅ Передаем cookies и аутентификационные данные
    };

    console.log("📌 Sending log data:", logData); // Логируем перед отправкой
    return this.http.post(`${this.apiUrl}/add`, logData, httpOptions);
  }

  // ✅ Получение всех логов (позже пригодится для админов)
  getLogs(): Observable<any[]> {
    const token = localStorage.getItem('token'); // ✅ Получаем токен
    const httpOptions = {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }),
      withCredentials: true
    };

    return this.http.get<any[]>(`${this.apiUrl}/all`, httpOptions);
  }
}
