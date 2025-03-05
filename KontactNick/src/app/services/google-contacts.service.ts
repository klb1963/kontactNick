import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GoogleContactsService {
  private googleContactsUrl = environment.googleContactsUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** ✅ Получение списка контактов из Google Contacts */
  getGoogleContacts(): Observable<any[]> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return of([]);
        }

        console.log("🔑 Access Token перед загрузкой контактов:", accessToken);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`
        });

        return this.http.get<any[]>(`${this.googleContactsUrl}/me/connections?personFields=names,emailAddresses`, { headers, withCredentials: true }).pipe(
          tap(contacts => console.log("✅ Загружены контакты из Google Contacts:", contacts)),
          catchError(error => {
            console.error(`❌ Ошибка при загрузке контактов: ${error.message}`, error);
            return of([]);
          })
        );
      })
    );
  }

  /** ✅ Добавление контакта в категорию (группу) Google Contacts */
  addToGoogleContacts(contact: any): Observable<any> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          alert("⚠️ Ошибка аутентификации! Войдите через Google.");
          return of(null);
        }

        console.log("🔑 Используем Google Access Token:", accessToken);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        return this.http.post(`http://localhost:8080/api/google/create-contact`, contact, { headers }).pipe(
          tap(response => console.log("✅ Контакт успешно добавлен в Google:", response)),
          catchError(error => {
            console.error("❌ Ошибка при добавлении контакта в Google:", error);
            alert("❌ Ошибка при добавлении контакта в Google. Проверьте права доступа.");
            return of(null);
          })
        );
      })
    );
  }


  addContactToGoogleCategory(resourceName: string, googleResourceName: string): Observable<any> {
    const url = `https://people.googleapis.com/v1/${resourceName}:updateContact`;

    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return throwError(() => new Error("❌ Нет Google Access Token"));
        }

        console.log("🔗 Добавляем контакт в категорию:", googleResourceName);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        const body = {
          memberships: [{ contactGroupMembership: { contactGroupResourceName: googleResourceName } }],
          updateMask: "memberships"
        };

        return this.http.patch(url, body, { headers }).pipe(
          tap(() => console.log("✅ Контакт добавлен в категорию!")),
          catchError(error => {
            console.error("❌ Ошибка при добавлении в категорию:", error);
            return throwError(() => error);
          })
        );
      })
    );
  }

}
