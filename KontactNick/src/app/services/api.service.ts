import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  constructor(private http: HttpClient, private authService: AuthService) {}

  /** Метод для GET-запросов */
  get<T>(url: string, withAuth: boolean = true): Observable<T> {
    return this.http.get<T>(url, this.getOptions(withAuth)).pipe(
      catchError(this.handleError)
    );
  }

  /** Метод для POST-запросов */
  post<T>(url: string, body: any, withAuth: boolean = true): Observable<T> {
    return this.http.post<T>(url, body, this.getOptions(withAuth)).pipe(
      catchError(this.handleError)
    );
  }

  /** Метод для DELETE-запросов */
  delete<T>(url: string, withAuth: boolean = true): Observable<T> {
    return this.http.delete<T>(url, this.getOptions(withAuth)).pipe(
      catchError(this.handleError)
    );
  }

  /** Формируем заголовки */
  private getOptions(withAuth: boolean) {
    let headers = new HttpHeaders();
    if (withAuth) {
      const token = this.authService.getGoogleAccessToken();
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
      }
    }
    return { headers, withCredentials: true };
  }

  /** Обработка ошибок */
  private handleError(error: any) {
    console.error('❌ API Error:', error);
    return throwError(() => new Error(error.message || 'Ошибка API'));
  }
}
