import { HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';
import {catchError, map, switchMap, tap} from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private baseUrl = environment.apiBaseUrl;
  private googleBaseUrl = `${this.baseUrl}/google/contact-groups`;

  constructor(private http: HttpClient,
              private authService: AuthService,
              private api: ApiService) {}

  /** ✅ Получение категории по ID */
  getCategoryById(categoryId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/contact-groups/${categoryId}`, { withCredentials: true }).pipe(
      tap(category => console.log("✅ Category loaded:", category)),
      catchError(error => {
        console.error("❌ Error fetching category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение полей категории */
  getCategoryFields(categoryId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/contact-groups/${categoryId}/fields`, { withCredentials: true }).pipe(
      tap(fields => console.log("📤 Fields loaded:", fields)),
      catchError(error => {
        console.error("❌ Error fetching fields:", error);
        return of([]);
      })
    );
  }

  /** ✅ Добавление поля в категорию */
  addFieldToCategory(categoryId: number, field: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact-groups/${categoryId}/fields`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field added")),
      catchError(error => {
        console.error("❌ Error adding field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Обновление поля */
  updateField(categoryId: number, fieldId: number, field: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/contact-groups/${categoryId}/fields/${fieldId}`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field updated")),
      catchError(error => {
        console.error("❌ Error updating field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление поля */
  deleteField(categoryId: number, fieldId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/contact-groups/${categoryId}/fields/${fieldId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field deleted")),
      catchError(error => {
        console.error("❌ Error deleting field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение списка категорий пользователя */
  getUserCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/contact-groups`, { withCredentials: true }).pipe(
      tap(categories => console.log("✅ Categories loaded:", categories)),
      catchError(error => {
        console.error("❌ Error fetching categories:", error);
        return of([]);
      })
    );
  }

  /** ✅ Создание новой категории */
  createCategory(category: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact-groups`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category created")),
      catchError(error => {
        console.error("❌ Error creating category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Обновление категории */
  updateCategory(categoryId: number, category: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/contact-groups/${categoryId}`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category updated")),
      catchError(error => {
        console.error("❌ Error updating category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление категории */
  deleteCategory(categoryId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/contact-groups/${categoryId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category deleted")),
      catchError(error => {
        console.error("❌ Error deleting category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Создание категории (группы) в Google Contacts */
  createGoogleCategory(category: { name: string }): Observable<any> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(token => {
        if (!token) {
          console.error("❌ Ошибка: не удалось получить Google Access Token!");
          return of(null);
        }

        console.log("✅ Используем Google Access Token, продолжаем запрос...");
        console.log("Google Access Token:", token);
        const headers = new HttpHeaders({
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json",
          "Accept": "application/json"
        });

        return this.http.post(`${this.googleBaseUrl}`, { contactGroup: { name: category.name } }, { headers }).pipe(
          tap(() => console.log("✅ Google category created")),
          catchError(error => {
            console.error("❌ Ошибка при создании Google категории:", error);
            return of(null);
          })
        );
      })
    );
  }

  /** ✅ Получение списка категорий (групп) из Google Contacts */
  getGoogleCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.googleBaseUrl}`, { withCredentials: true }).pipe(
      tap(categories => console.log("✅ Google categories loaded:", categories)),
      catchError(error => {
        console.error("❌ Error fetching Google categories:", error);
        return of([]);
      })
    );
  }

  /** ✅ Добавление контакта в категорию Google Contacts */
  addContactToGoogleCategory(categoryId: string, contactResourceName: string): Observable<any> {
    return this.http.post(`${this.googleBaseUrl}/${categoryId}/add`, { resourceName: contactResourceName }, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Contact added to Google category")),
      catchError(error => {
        console.error("❌ Error adding contact to Google category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление контакта из категории Google Contacts */
  removeContactFromGoogleCategory(categoryId: string, contactResourceName: string): Observable<any> {
    return this.http.post(`${this.googleBaseUrl}/${categoryId}/remove`, { resourceName: contactResourceName }, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Contact removed from Google category")),
      catchError(error => {
        console.error("❌ Error removing contact from Google category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Добавление контакта и категории в Google Contacts */
  addContactToCategory(categoryId: number, contact: any) {
    return this.http.post(`${this.googleBaseUrl}/${categoryId}/contacts`, { contact }, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Contact added to category")),
      catchError(error => {
        console.error("❌ Error adding contact to category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение google_resource_name для категории */
  getGoogleResourceName(categoryId: number): Observable<string> {
    return this.authService.getValidJwtAccessToken().pipe(
      switchMap(jwtAccessToken => {
        console.log("🔍 Используем JWT Access Token:", jwtAccessToken);

        if (!jwtAccessToken) {
          console.error("❌ Ошибка: отсутствует JWT Access Token!");
          return throwError(() => new Error("Не удалось получить Google Resource Name: отсутствует токен"));
        }

        return this.http.get<{ google_resource_name: string }>(
          `${this.baseUrl}/contact-groups/${categoryId}/google-resource-name`, // ✅ ПРАВИЛЬНЫЙ URL
          {
            headers: new HttpHeaders({ Authorization: `Bearer ${jwtAccessToken}` }),
            withCredentials: true
          }
        );
      }),
      tap(response => console.log("🔍 Ответ от сервера:", response)), // ✅ Логируем ответ перед обработкой
      map(response => (response as { google_resource_name: string }).google_resource_name), // ✅ Фикс ошибки типа
      catchError(error => {
        console.error("❌ Ошибка получения Google Resource Name:", error);
        return throwError(() => new Error("Не удалось получить Google Resource Name"));
      })
    );
  }

}
