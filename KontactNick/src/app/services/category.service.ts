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

  getCategoryById(categoryId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/categories/${categoryId}`, { withCredentials: true }).pipe(
      tap(category => console.log("✅ Category loaded:", category)),
      catchError(error => {
        console.error("❌ Error fetching category:", error);
        return of(null);
      })
    );
  }

  getCategoryFields(categoryId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories/${categoryId}/fields`, { withCredentials: true }).pipe(
      tap(fields => console.log("📤 Fields loaded:", fields)),
      catchError(error => {
        console.error("❌ Error fetching fields:", error);
        return of([]);
      })
    );
  }

  addFieldToCategory(categoryId: number, field: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/categories/${categoryId}/field`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field added")),
      catchError(error => {
        console.error("❌ Error adding field:", error);
        return of(null);
      })
    );
  }

  updateField(categoryId: number, fieldId: number, field: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/categories/${categoryId}/fields/${fieldId}`, field, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field updated")),
      catchError(error => {
        console.error("❌ Error updating field:", error);
        return of(null);
      })
    );
  }

  deleteField(categoryId: number, fieldId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}/fields/${fieldId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Field deleted")),
      catchError(error => {
        console.error("❌ Error deleting field:", error);
        return of(null);
      })
    );
  }

  getUserCategories(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/categories`, { withCredentials: true }).pipe(
      tap(categories => console.log("✅ Categories loaded:", categories)),
      catchError(error => {
        console.error("❌ Error fetching categories:", error);
        return of([]);
      })
    );
  }

  createCategory(category: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/categories`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category created")),
      catchError(error => {
        console.error("❌ Error creating category:", error);
        return of(null);
      })
    );
  }

  updateCategory(categoryId: number, category: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/categories/${categoryId}`, category, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category updated")),
      catchError(error => {
        console.error("❌ Error updating category:", error);
        return of(null);
      })
    );
  }

  deleteCategory(categoryId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}`, { withCredentials: true }).pipe(
      tap(() => console.log("✅ Category deleted")),
      catchError(error => {
        console.error("❌ Error deleting category:", error);
        return of(null);
      })
    );
  }
}
