import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../services/category.service';
import { AuthService } from '../services/auth.service';
import { GoogleContactsService } from '../services/google-contacts.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { map, switchMap, take, tap } from 'rxjs/operators';
import { catchError, throwError } from 'rxjs';

@Component({
  selector: 'app-add-contact-dialog',
  templateUrl: './add-contact.component.html',
  standalone: true,
  styleUrls: ['./add-contact.component.css'],
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDialogModule
  ]
})
export class AddContactDialogComponent implements OnInit {
  categoryId: number = 0;
  categoryName: string = '';
  fields: any[] = [];
  contactData: any = { name: '', nick: '', email: '', phone: '', otherFields: {} };
  currentUserNick: string = '';

  constructor(
    public dialogRef: MatDialogRef<AddContactDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private categoryService: CategoryService,
    private authService: AuthService,
    private googleContactsService: GoogleContactsService
  ) {
    console.log("📥 Received data in constructor:", data);

    if (typeof data?.category === 'object') {
      this.categoryId = data.category.id ?? 0;
      this.categoryName = data.category.name ?? 'Unknown';
    } else {
      console.error("❌ Ошибка: `category` передан как строка, а не объект!", data.category);
    }
  }

  ngOnInit(): void {
    console.log("🛠️ Dialog opened with category:", this.categoryName, "ID:", this.categoryId);

    this.authService.getUserProfile().subscribe(profile => {
      if (profile?.nick) {
        this.currentUserNick = profile.nick;
      }
    });

    this.loadFields();
  }

  loadFields(): void {
    if (!this.categoryId) {
      console.error("❌ Ошибка: categoryId не передан!");
      return;
    }

    console.log("📡 Fetching fields for categoryId:", this.categoryId);

    this.categoryService.getCategoryFields(this.categoryId).subscribe({
      next: (fields) => {
        this.fields = fields;
        console.log("✅ Fields loaded:", fields);
      },
      error: (err: any) => {
        console.error("❌ Error loading fields:", err);
      }
    });
  }

  save(): void {
    if (!this.contactData.name?.trim() || !this.contactData.email?.trim()) {
      alert("⚠️ Please enter a valid name and email!");
      return;
    }

    if (!this.categoryId) {
      alert("⚠️ Ошибка: категория не выбрана!");
      return;
    }

    console.log("🔄 Проверяем access_token перед отправкой контакта...");

    this.authService.getGoogleAccessToken().pipe(
      take(1),
      tap(accessToken => {
        if (!accessToken) {
          throw new Error("❌ Ошибка: нет валидного Google Access Token!");
        }
        console.log("✅ Используем Google Access Token:", accessToken);
      }),
      switchMap(accessToken =>
        this.categoryService.getGoogleResourceName(this.categoryId).pipe(
          tap(googleResourceName => {
            console.log("✅ Получен Google Resource Name:", googleResourceName);
          }),
          map(googleResourceName => ({ accessToken, googleResourceName })),
          catchError(error => {
            console.error("❌ Ошибка получения Google Resource Name:", error);
            alert("❌ Ошибка: не удалось получить Google Resource Name.");
            return throwError(() => error);
          })
        )
      )
    ).subscribe({
      next: ({ accessToken, googleResourceName }) => {
        console.log("✅ Получен Google Resource Name:", googleResourceName);

        const googleContact: any = {
          names: [{ givenName: this.contactData.name }],
          emailAddresses: [{ value: this.contactData.email }],
          memberships: [{ contactGroupMembership: { contactGroupResourceName: googleResourceName } }]
        };

        if (this.contactData.phone?.trim()) {
          googleContact.phoneNumbers = [{ value: this.contactData.phone }];
        }

        const userDefinedFields = this.fields
          .map(field => {
            const fieldValue = this.contactData.otherFields[field.name]?.trim();
            return fieldValue ? { key: field.name, value: fieldValue } : null;
          })
          .filter(field => field !== null);

        if (userDefinedFields.length > 0) {
          googleContact.userDefined = userDefinedFields;
        }

        console.log("📤 Итоговый JSON перед отправкой:", googleContact);
        this.saveContactToGoogle(googleContact, googleResourceName);
      },
      error: (error: any) => {
        console.error("❌ Ошибка при обработке контакта:", error);
        alert(error.message || "❌ Произошла ошибка! Проверьте настройки.");
      }
    });
  }

  saveContactToGoogle(contact: any, googleResourceName: string): void {
    this.googleContactsService.addToGoogleContacts(contact).subscribe({
      next: (response) => {
        console.log("✅ Contact created in Google Contacts!", response);

        if (response && response.resourceName) {
          console.log("🔗 Contact resourceName:", response.resourceName);

          console.log("📂 Adding contact to category:", googleResourceName);

          this.googleContactsService.addContactToGoogleCategory(response.resourceName, googleResourceName).subscribe({
            next: () => {
              console.log("✅ Contact successfully added to Google Category!");
              alert("✅ Контакт успешно добавлен в Google и привязан к категории!");

              this.contactData.contactGroupResourceName = googleResourceName;
              this.dialogRef.close(this.contactData);
            },
            error: (err: any) => {
              console.error("❌ Error adding contact to category:", err);
              alert("❌ Контакт создан, но не добавлен в категорию.");
            }
          });
        } else {
          console.error("❌ Ошибка: Google API не вернул resourceName!", response);
          alert("❌ Ошибка: Google API не вернул resourceName.");
        }
      },
      error: (err: any) => {
        console.error("❌ Ошибка при добавлении контакта в Google:", err);
        alert("❌ Ошибка при добавлении контакта в Google.");
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }

}
