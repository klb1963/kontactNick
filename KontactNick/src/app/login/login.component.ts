import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { first } from 'rxjs';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object // ✅ Проверяем, браузер или сервер
  ) {}

  ngOnInit(): void {
    console.log('🟢 LoginComponent initialized');

    // ✅ Проверяем, выполняется ли код в браузере (избегаем SSR-ошибки)
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.authService.checkAuth().pipe(first()).subscribe({
          next: (isAuthenticated: boolean) => {
            if (isAuthenticated) {
              console.log('🍪 Token найден, редирект на Dashboard');
              this.router.navigate(['/dashboard']);
            } else {
              console.warn('🚨 Нет токена, ждем входа пользователя');
            }
          },
          error: (err) => {
            console.warn('❌ Ошибка проверки авторизации:', err);
          }
        });
      }, 100); // 🔴 Даем время загрузиться после SSR
    } else {
      console.warn("❌ Код выполняется в SSR (на сервере), Cookies недоступны.");
    }
  }

  loginWithGoogle(): void {
    console.log('🔵 Redirecting to Google login...');
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }

  login(): void {
    if (!this.email || !this.password) {
      this.errorMessage = '⚠️ Email и пароль обязательны!';
      return;
    }

    this.authService.login(this.email, this.password).pipe(first()).subscribe({
      next: () => {
        console.log('✅ Login successful, checking authentication...');
        this.authService.checkAuth().pipe(first()).subscribe({
          next: (isAuthenticated: boolean) => {
            if (isAuthenticated) {
              console.log('✅ Пользователь аутентифицирован, переходим на Dashboard');
              this.router.navigate(['/dashboard']);
            } else {
              console.warn('❌ Вход выполнен, но токен не найден. Проверь сервер.');
            }
          },
          error: (err) => {
            console.error('🚨 Ошибка после логина:', err);
          }
        });
      },
      error: (err) => {
        console.error('🚨 Login error:', err);
        this.errorMessage = '🚨 Ошибка входа. Проверьте email и пароль.';
      },
    });
  }
}
