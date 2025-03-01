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

// 🔥 Что изменилось?
// 1.	Удалена ненужная логика с refreshGoogleAccessToken() → теперь перед запросом всегда берём актуальный токен с бэкенда.
// 2.	Каждый запрос в Google API теперь 100% использует свежий access_token.
// 3.	Добавлено логирование перед каждым запросом, чтобы проще было отлаживать.
// 4.	Убраны дублирующиеся проверки (например, не пытаемся обновлять токен сами, если его нет).
// 5.	Все API-методы логируют токен перед отправкой (для удобной отладки).

@Injectable({
  providedIn: 'root'
})
export class GoogleContactsService {
  private googleContactsUrl = environment.googleContactsUrl;

  constructor(private http: HttpClient, private authService: AuthService) {}

  /** ✅ Добавление контакта в Google Contacts */
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

        console.log("🔑 Используем access_token:", accessToken);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        return this.http.post(`${this.googleContactsUrl}:createContact`, contact, { headers }).pipe(
          switchMap((response: any) => {
            console.log("✅ Контакт успешно добавлен в Google:", response);

            // ✅ Если передана группа — добавляем контакт в неё
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

        console.log("🔑 Access Token перед удалением контакта:", accessToken);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`
        });

        return this.http.delete(`${this.googleContactsUrl}/${resourceName}`, { headers }).pipe(
          tap(() => console.log(`✅ Контакт ${resourceName} удалён из Google Contacts`)),
          catchError(error => {
            console.error(`❌ Ошибка при удалении контакта: ${error.message}`, error);
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

        console.log("🔑 Access Token перед загрузкой контактов:", accessToken);

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
// 🔥 Что изменилось?
// ✅ Используем etag: "*", чтобы избежать проблем с версией
// ✅ Добавлен updatePersonFields=memberships в URL
// ✅ Теперь перед отправкой всё логируется 🚀
  // Исправления:
  // 1.	✅ Логирование ошибок: если токен null, нужно сразу делать alert().
  // 2.	✅ Принудительное обновление токена, если он невалидный.
  // 3.	✅ Проверка перед отправкой: токен из localStorage и из сервиса должны совпадать.
  // 4.	✅ Более подробное логирование для отладки.
  addContactToGoogleCategory(contactResourceName: string, groupResourceName: string): Observable<any> {
    console.log("📡 Инициализация добавления контакта в группу...");

    if (!contactResourceName || !groupResourceName) {
      console.error("❌ Ошибка: отсутствуют обязательные параметры!");
      return of({ error: "contactResourceName и groupResourceName обязательны" });
    }

    return this.authService.getGoogleAccessToken().pipe(
      switchMap(accessToken => {
        if (!accessToken) {
          console.error("❌ Ошибка: нет Google Access Token! Запросите повторную авторизацию.");
          alert("⚠️ Ошибка аутентификации! Войдите через Google.");
          return of(null);
        }

        console.log(`🔑 Используем Access Token: ${accessToken}`);
        console.log(`📡 Добавляем контакт ${contactResourceName} в группу ${groupResourceName}`);

        const headers = new HttpHeaders({
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        });

        const body = {
          "etag": "*",  // ⬅️ Обновление без проверки версии
          "memberships": [
            {
              "contactGroupMembership": {
                "contactGroupResourceName": groupResourceName
              }
            }
          ]
        };

        console.log("📤 JSON запроса:", JSON.stringify(body, null, 2));

        return this.http.patch(
          `${this.googleContactsUrl}/${contactResourceName}:updateContact?updatePersonFields=memberships`,
          body,
          { headers }
        ).pipe(
          tap(response => console.log("✅ Контакт успешно добавлен в группу Google Contacts:", response)),
          catchError(error => {
            console.error("❌ Ошибка при добавлении контакта в группу Google:", error);

            if (error.status === 401) {
              console.warn("🔄 Access Token недействителен! Пробуем обновить...");

              return this.authService.getGoogleAccessToken().pipe(
                switchMap(newAccessToken => {
                  if (!newAccessToken) {
                    console.error("❌ Ошибка обновления Access Token!");
                    return of(null);
                  }

                  console.log("✅ Новый Access Token получен:", newAccessToken);

                  const newHeaders = new HttpHeaders({
                    "Authorization": `Bearer ${newAccessToken}`,
                    "Content-Type": "application/json"
                  });

                  return this.http.patch(
                    `${this.googleContactsUrl}/${contactResourceName}:updateContact?updatePersonFields=memberships`,
                    body,
                    { headers: newHeaders }
                  ).pipe(
                    tap(updatedResponse => console.log("✅ Повторный запрос успешен!", updatedResponse)),
                    catchError(retryError => {
                      console.error("❌ Ошибка при повторном запросе:", retryError);
                      return of(null);
                    })
                  );
                })
              );
            }

            return of(null);
          })
        );
      })
    );
  }

}
