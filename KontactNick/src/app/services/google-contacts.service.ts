import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

// 🔥 Что улучшилось?
// ✅ Теперь можно сразу добавить контакт в группу при создании
// 	•	Если передан groupResourceName, контакт будет сразу добавлен в группу после создания.
// 	•	Если groupResourceName не указан → просто создаст контакт.
// ✅ Добавили метод addContactToGoogleCategory(...)
// 	•	Теперь можно добавлять уже существующие контакты в группу Google.

@Injectable({
  providedIn: 'root'
})
export class GoogleContactsService {
  private googleContactsUrl = environment.googleContactsUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** ✅ Добавление контакта в Google Contacts (и в группу) */
  addToGoogleContacts(contact: any, groupResourceName?: string): Observable<any> {
    if (!contact || Object.keys(contact).length === 0) {
      console.error("❌ Ошибка: передан пустой объект контакта!");
      return of(null);
    }

    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          alert("⚠️ Ошибка аутентификации! Войдите через Google.");
          return of(null);
        }

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        console.log("📡 Отправка контакта в Google:", contact);

        return this.http.post(`${this.googleContactsUrl}:createContact`, contact, { headers }).pipe(
          switchMap((response: any) => {
            console.log("✅ Контакт успешно добавлен в Google:", response);

            // Если передана группа - добавляем контакт в неё
            if (groupResourceName) {
              return this.addContactToGoogleCategory(response.resourceName, groupResourceName);
            }
            return of(response);
          }),
          catchError(error => {
            console.error(`❌ Ошибка при добавлении контакта в Google: ${error.message}`, error);
            alert("❌ Ошибка при добавлении контакта в Google. Проверьте права доступа.");
            return of(null);
          })
        );
      })
    );
  }

  /** ✅ Удаление контакта из Google Contacts */
  deleteFromGoogleContacts(resourceName: string): Observable<any> {
    if (!resourceName) {
      console.error("❌ Ошибка: resourceName не указан для удаления!");
      return of(null);
    }

    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token!");
          return of(null);
        }

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`
        });

        return this.http.delete(`${this.googleContactsUrl}/${resourceName}`, { headers }).pipe(
          tap(() => console.log(`✅ Контакт ${resourceName} удалён из Google Contacts`)),
          catchError(error => {
            console.error(`❌ Ошибка при удалении контакта (${resourceName}): ${error.message}`, error);
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
            console.error(`❌ Ошибка при загрузке контактов: ${error.message}`, error);
            return of([]);
          })
        );
      })
    );
  }

  /** ✅ Добавление контакта в категорию (группу) Google Contacts */
  addContactToGoogleCategory(contactResourceName: string, groupResourceName: string): Observable<any> {
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

        const body = {
          "memberships": [
            {
              "contactGroupMembership": {
                "contactGroupResourceName": groupResourceName
              }
            }
          ]
        };

        console.log(`📡 Добавляем контакт ${contactResourceName} в группу ${groupResourceName}`);

        return this.http.patch(`${this.googleContactsUrl}/${contactResourceName}:updateContact`, body, { headers }).pipe(
          tap(() => console.log("✅ Контакт успешно добавлен в группу Google Contacts")),
          catchError(error => {
            console.error("❌ Ошибка при добавлении контакта в группу Google:", error);
            return of(null);
          })
        );
      })
    );
  }
}
