import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { first } from 'rxjs';

@Component({
  selector: 'app-user-registration',
  templateUrl: './user-registration.component.html',
  styleUrls: ['./user-registration.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
    RouterLink,
  ],
})
export class UserRegistrationComponent {
  email = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  register(): void {
    if (this.password !== this.confirmPassword) {
      this.errorMessage = '⚠️ Пароли не совпадают!';
      return;
    }

    this.authService.register(this.email, this.password).pipe(first()).subscribe({
      next: () => {
        console.log('✅ Регистрация успешна, проверяем авторизацию...');
        this.authService.checkAuth().pipe(first()).subscribe({
          next: (isAuthenticated: boolean) => {
            if (isAuthenticated) {
              console.log('✅ Пользователь зарегистрирован и аутентифицирован, переходим на Dashboard');
              this.router.navigate(['/dashboard']);
            } else {
              console.warn('❌ Регистрация прошла, но токен не найден. Проверь сервер.');
              this.router.navigate(['/login']);
            }
          },
          error: (err) => {
            console.error('🚨 Ошибка после регистрации:', err);
            this.errorMessage = '❌ Ошибка входа после регистрации.';
          }
        });
      },
      error: (err) => {
        console.error('🚨 Ошибка регистрации:', err);
        this.errorMessage = '❌ Ошибка регистрации. Проверьте данные и попробуйте снова.';
      },
    });
  }

  // ✅ Метод для регистрации через Google
  registerWithGoogle(): void {
    console.log('🔵 Redirecting to Google registration...');
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }
}
