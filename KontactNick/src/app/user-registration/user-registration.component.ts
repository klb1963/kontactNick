import { Component } from '@angular/core';
import { Router } from '@angular/router';
import {MatFormField, MatFormFieldModule} from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-user-registration',
  templateUrl: './user-registration.component.html',
  styleUrls: ['./user-registration.component.css'],
  imports: [
    MatFormField,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    ReactiveFormsModule
  ],
  standalone: true
})
export class UserRegistrationComponent {
  constructor(private router: Router) {}

  // Перенаправление на BE для авторизации Google
  googleLogin() {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }

  // Опционально: регистрация с полями
  register(email: string) {
    // Обработка регистрации
    console.log('Register user with email:', email);
  }
}
