import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule, isPlatformBrowser } from '@angular/common';

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
        const cookieToken = this.getTokenFromCookies();
        if (cookieToken) {
          console.log('🍪 Token found in Cookies, redirecting to dashboard:', cookieToken);
          this.router.navigate(['/dashboard']);
        } else {
          console.warn('🚨 No token found in Cookies, waiting for user action');
        }
      }, 100); // 🔴 Даем время загрузиться после SSR
    } else {
      console.warn("❌ Код выполняется в SSR (на сервере), Cookies недоступны.");
    }
  }

  private getTokenFromCookies(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      console.warn("❌ `document` недоступен (SSR или серверная среда)");
      return null;
    }

    const cookies = document.cookie.split('; ');
    for (const cookie of cookies) {
      const [name, value] = cookie.split('=');
      if (name === 'jwt-token') {
        return value;
      }
    }
    return null;
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

    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        console.log('✅ Login successful, reloading to fetch Cookies');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('🚨 Login error:', err);
        this.errorMessage = '🚨 Ошибка входа. Проверьте email и пароль.';
      },
    });
  }
}
