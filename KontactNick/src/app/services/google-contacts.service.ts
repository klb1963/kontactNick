import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'  // ✅ Обязательно для Standalone API
})
export class GoogleContactsService {
  constructor(private http: HttpClient) {}

  addToGoogleContacts(contact: any, accessToken: string | null): Observable<any> {
    if (!accessToken) {
      console.error("❌ No Google Access Token available!");
      return new Observable(observer => {
        observer.error("No Access Token");
        observer.complete();
      });
    }

    const headers = new HttpHeaders({
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    });

    const apiUrl = 'https://people.googleapis.com/v1/people:createContact';

    return this.http.post(apiUrl, contact, { headers });
  }

}
