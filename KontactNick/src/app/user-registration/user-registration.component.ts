import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-user-registration',
  templateUrl: './user-registration.component.html',
  styleUrls: ['./user-registration.component.css'],
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
    ReactiveFormsModule
  ],
  standalone: true
})
export class UserRegistrationComponent implements OnInit {
  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit(): void {}

  // Метод для регистрации пользователя
  register(email: string): void {
    console.log(`Registering user with email: ${email}`);
    // Здесь вы можете добавить логику для регистрации пользователя
  }

  // Метод для аутентификации через Google
  googleLogin(): void {
    console.log('Google login initiated');
    // Пример: используем AuthService для авторизации
    this.authService.getTokenFromServer().subscribe({
      next: (token) => {
        this.authService.saveToken(token); // Сохраняем токен
        this.router.navigate(['/dashboard']); // Перенаправляем пользователя
      },
      error: (err) => {
        console.error('Error during Google login:', err);
      }
    });
  }
}
