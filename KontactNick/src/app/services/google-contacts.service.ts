import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

// 🔥 Что изменилось?
//   1.	Использует AuthService для динамического получения токена, теперь не нужно передавать его вручную.
// 2.	Добавлен метод deleteFromGoogleContacts, который позволяет удалять контакты из Google Contacts.
// 3.	Добавлен метод getGoogleContacts, который загружает список контактов пользователя.
// 4.	Используется environment.ts, чтобы легко менять baseUrl.
// 5.	Добавлена обработка ошибок через catchError(), теперь сервис не ломается при ошибках API.

@Injectable({
  providedIn: 'root'
})
export class GoogleContactsService {
  private googleContactsUrl = 'https://people.googleapis.com/v1/people';

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** ✅ Добавление контакта в Google Contacts */
  addToGoogleContacts(contact: any): Observable<any> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return of(null);
        }

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        return this.http.post(`${this.googleContactsUrl}:createContact`, contact, { headers }).pipe(
          tap(() => console.log("✅ Контакт добавлен в Google Contacts")),
          catchError(error => {
            console.error("❌ Ошибка при добавлении контакта:", error);
            return of(null);
          })
        );
      })
    );
  }

  /** ✅ Удаление контакта из Google Contacts */
  deleteFromGoogleContacts(resourceName: string): Observable<any> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return of(null);
        }

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`
        });

        return this.http.delete(`${this.googleContactsUrl}/${resourceName}:deleteContact`, { headers }).pipe(
          tap(() => console.log("✅ Контакт удалён из Google Contacts")),
          catchError(error => {
            console.error("❌ Ошибка при удалении контакта:", error);
            return of(null);
          })
        );
      })
    );
  }

  /** ✅ Получение списка контактов из Google Contacts */
  getGoogleContacts(): Observable<any[]> {
    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return of([]);
        }

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`
        });

        return this.http.get<any[]>(`${this.googleContactsUrl}/me/connections?personFields=names,emailAddresses`, { headers }).pipe(
          tap(contacts => console.log("✅ Загружены контакты из Google Contacts:", contacts)),
          catchError(error => {
            console.error("❌ Ошибка при загрузке контактов:", error);
            return of([]);
          })
        );
      })
    );
  }
}
