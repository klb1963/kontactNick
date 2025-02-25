import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  /** ✅ Получение категории по ID */
  getCategoryById(categoryId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/categories/${categoryId}`, { withCredentials: true }).pipe(
      tap(category => console.log("✅ Category loaded:", category)),
      catchError(error => {
        console.error("❌ Error fetching category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение полей категории */
  getCategoryFields(categoryId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories/${categoryId}/fields`, { withCredentials: true }).pipe(
      tap(fields => console.log("📤 Fields loaded:", fields)),
      catchError(error => {
        console.error("❌ Error fetching fields:", error);
        return of([]);
      })
    );
  }

  /** ✅ Добавление поля в категорию */
  addFieldToCategory(categoryId: number, field: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/categories/${categoryId}/field`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field added")),
      catchError(error => {
        console.error("❌ Error adding field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Обновление поля */
  updateField(categoryId: number, fieldId: number, field: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/categories/${categoryId}/fields/${fieldId}`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field updated")),
      catchError(error => {
        console.error("❌ Error updating field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление поля */
  deleteField(categoryId: number, fieldId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}/fields/${fieldId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field deleted")),
      catchError(error => {
        console.error("❌ Error deleting field:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение списка категорий пользователя */
  getUserCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories`, { withCredentials: true }).pipe(
      tap(categories => console.log("✅ Categories loaded:", categories)),
      catchError(error => {
        console.error("❌ Error fetching categories:", error);
        return of([]);
      })
    );
  }

  /** ✅ Создание новой категории */
  createCategory(category: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/categories`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category created")),
      catchError(error => {
        console.error("❌ Error creating category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Обновление категории */
  updateCategory(categoryId: number, category: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/categories/${categoryId}`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category updated")),
      catchError(error => {
        console.error("❌ Error updating category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление категории */
  deleteCategory(categoryId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category deleted")),
      catchError(error => {
        console.error("❌ Error deleting category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Создание категории (группы) в Google Contacts */
  createGoogleCategory(category: { name: string }): Observable<any> {
    return this.http.post(`${this.baseUrl}/google/categories`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Google category created")),
      catchError(error => {
        console.error("❌ Error creating Google category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Получение списка категорий (групп) из Google Contacts */
  getGoogleCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/google/categories`, { withCredentials: true }).pipe(
      tap(categories => console.log("✅ Google categories loaded:", categories)),
      catchError(error => {
        console.error("❌ Error fetching Google categories:", error);
        return of([]);
      })
    );
  }

  /** ✅ Добавление контакта в категорию Google Contacts */
  addContactToGoogleCategory(categoryId: string, contactResourceName: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/google/categories/${categoryId}/add`, { resourceName: contactResourceName }, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Contact added to Google category")),
      catchError(error => {
        console.error("❌ Error adding contact to Google category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Удаление контакта из категории Google Contacts */
  removeContactFromGoogleCategory(categoryId: string, contactResourceName: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/google/categories/${categoryId}/remove`, { resourceName: contactResourceName }, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Contact removed from Google category")),
      catchError(error => {
        console.error("❌ Error removing contact from Google category:", error);
        return of(null);
      })
    );
  }

  /** ✅ Добавление контакта и категории в Google Contacts */
  addContactToCategory(categoryId: number, contact: any) {
    return this.http.post(`/api/google-contacts/add`, {
      categoryId,
      contact
    });
  }

}
