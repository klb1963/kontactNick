import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field'; // ✅ Материальный модуль для форм
import { MatInputModule } from '@angular/material/input'; // ✅ Материальный модуль для инпутов
import { MatButtonModule } from '@angular/material/button'; // ✅ Материальный модуль для кнопок
import { CommonModule } from '@angular/common'; // ✅ Для базовых директив Angular

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatFormFieldModule, // ✅ Добавлен модуль Material FormField
    MatInputModule, // ✅ Добавлен модуль Material Input
    MatButtonModule, // ✅ Добавлен модуль Material Button
  ],
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {

    console.log('🟢 LoginComponent initialized');

    // ✅ Проверяем, есть ли токен в localStorage перед входом
    const storedToken = localStorage.getItem('authToken');
    console.log('📂 localStorage authToken (before login):', storedToken);

    const token = this.getTokenFromUrl();

    if (token) {
      console.log('✅ Token found in URL:', token);
      console.log('💾 Saving token:', token);
      this.authService.saveToken(token);
      this.authService.saveToken(token);
      this.clearQueryParams();
      this.router.navigate(['/dashboard']);
      return; // ⬅️ Дальше код не выполняем!
    }

    // const storedToken = this.authService.getToken();

    if (storedToken) {
      console.log('✅ Token found in localStorage, redirecting...');
      this.router.navigate(['/dashboard']);
      return; // ⬅️ Дальше код не выполняем!
    }

    console.warn('🚨 No token in URL or localStorage, waiting for user action');

  }

  private getTokenFromUrl(): string | null {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('token');
  }

  private clearQueryParams(): void {
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  loginWithGoogle(): void {
    console.log('🔵 Redirecting to Google login...');
    window.location.href = 'http://localhost:8080/oauth2/authorization/google'; //
  }

  login(): void {
    if (!this.email || !this.password) {
      this.errorMessage = '⚠️ Email и пароль обязательны!';
      return;
    }

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        console.log('✅ Login successful:', response);
        this.authService.saveToken(response.token);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('🚨 Login error:', err);
        this.errorMessage = '🚨 Ошибка входа. Проверьте email и пароль.';
      },
    });
  }
}
