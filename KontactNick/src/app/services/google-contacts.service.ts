import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({
  providedIn: 'root'  // ✅ Обязательно для Standalone API
})
export class GoogleContactsService {
  constructor(private http: HttpClient) {}

  addToGoogleContacts(contact: any, additionalFields: any[], accessToken: string) {
    const headers = new HttpHeaders().set("Authorization", `Bearer ${accessToken}`);

    const requestData = {
      ...contact,  // ✅ Основные поля (name, nickname, email, phone)
      additionalFields  // ✅ Дополнительные поля в виде списка
    };

    console.log("📤 Sending to Google Contacts:", requestData);

    return this.http.post('/api/google/contacts', requestData, { headers });
  }
}
