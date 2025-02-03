import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { map, tap, switchMap, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  getTokenFromServer(): Observable<string | null> {
    return this.http.get<{ token?: string }>(`${this.baseUrl}/auth/token`, {
      withCredentials: true
    }).pipe(
      map(response => response?.token ?? null),
      catchError(error => {
        if (error.status !== 401) {
          console.error("🚨 Unexpected error fetching token:", error);
        }
        return of(null);
      })
    );
  }

  isLoggedIn(): Observable<boolean> {
    return this.getTokenFromServer().pipe(
      map(token => !!token),
      catchError(() => of(false))
    );
  }

  getUserCategories(): Observable<any[]> {
    return this.getTokenFromServer().pipe(
      switchMap(token => {
        if (!token) {
          console.warn("ℹ️ User not authenticated, category request skipped");
          return of([]);
        }
        return this.http.get<any[]>(`${this.baseUrl}/categories`, { withCredentials: true }).pipe(
          tap(categories => console.log('📂 Categories received:', categories)),
          catchError(error => {
            console.error('🚨 Error fetching categories:', error);
            return of([]);
          })
        );
      })
    );
  }

  createCategory(category: { name: string, description: string }) {
    return this.http.post(`${this.baseUrl}/categories`, category, { withCredentials: true })
      .pipe(catchError(error => {
        console.error('🚨 Error creating category:', error);
        return of(null);
      }));
  }

  updateCategory(id: number, category: { name: string, description: string }) {
    return this.http.put(`${this.baseUrl}/categories/${id}`, category, { withCredentials: true })
      .pipe(catchError(error => {
        console.error('🚨 Error updating category:', error);
        return of(null);
      }));
  }

  deleteCategory(categoryId: number) {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}`, { withCredentials: true });
  }

  getCategoryById(categoryId: number) {
    return this.http.get<any>(`${this.baseUrl}/categories/${categoryId}`, {
      withCredentials: true
    });
  }

  addFieldToCategory(categoryId: number, field: { name: string, fieldType: string, value: string }) {
    console.log("🌐 Sending request to add field:", field);
    return this.http.post(`${this.baseUrl}/categories/${categoryId}/fields`, field, {
      withCredentials: true
    }).pipe(
      tap(response => console.log("✅ Field added successfully:", response)),
      catchError(error => {
        console.error("❌ Error adding field:", error);
        return throwError(() => error);
      })
    );
  }

  getCategoryFields(categoryId: number) {
    console.log("🌐 Sending request to fetch fields for category ID:", categoryId);
    return this.http.get<any[]>(`${this.baseUrl}/categories/${categoryId}/fields`, {
      withCredentials: true
    }).pipe(
      tap(fields => console.log("📤 Server response (fields received):", fields)),
      catchError(error => {
        console.error("❌ Error in getCategoryFields (HTTP Error):", error);
        console.warn("⚠️ Returning an empty array due to error.");
        return of([]);
      })
    );
  }

  deleteField(categoryId: number, fieldId: number) {
    return this.http.delete(`${this.baseUrl}/categories/${categoryId}/fields/${fieldId}`, { withCredentials: true });
  }

  updateField(categoryId: number, fieldId: number, updatedField: { name: string, fieldType: string, value: string }) {
    return this.http.put(`http://localhost:8080/api/categories/${categoryId}/fields/${fieldId}`, updatedField, { withCredentials: true });
  }

  getCurrentUserEmail(): Observable<string | null> {
    return this.getTokenFromServer().pipe(
      map(token => {
        if (token) {
          try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.email || payload.sub || null;
          } catch (error) {
            console.error('🚨 Error decoding token:', error);
            return null;
          }
        }
        return null;
      }),
      catchError(error => {
        console.error('🚨 Error fetching user email:', error);
        return of(null);
      })
    );
  }

  /** ✅ Проверка статуса аутентификации */
  public checkAuthStatus(): Observable<boolean> {
    return this.http.get<{ authenticated?: boolean }>(
      `http://localhost:8080/api/auth/check`,
      { withCredentials: true }
    ).pipe(
      map((response: any) => response?.authenticated === 'true'),
      catchError((error: any) => {
        if (error.status !== 401) {
          console.error('🚨 Unexpected auth check error:', error);
        }
        return of(false);
      })
    );
  }

  /** ✅ Логин */
  public login(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `http://localhost:8080/api/auth/login`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      tap(() => console.log('✅ Login successful, token should be in cookies.')),
      switchMap(() => this.checkAuthStatus()),
      catchError((error: any) => {
        console.error('🚨 Login error:', error);
        return of(false);
      })
    );
  }

  /** ✅ Регистрация */
  public register(email: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(
      `http://localhost:8080/api/auth/register`,
      { email, password },
      { withCredentials: true }
    ).pipe(
      switchMap(() => this.checkAuthStatus()),
      catchError((error: any) => {
        console.error('🚨 Registration error:', error);
        return of(false);
      })
    );
  }

}
