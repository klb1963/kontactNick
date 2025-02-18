import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { first } from 'rxjs';

@Component({
  selector: 'app-user-login',
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
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    console.log('🟢 LoginComponent initialized');

    if (isPlatformBrowser(this.platformId)) {
      this.authService.checkAuthStatus().pipe(first()).subscribe({
        next: (isAuthenticated: boolean) => {
          if (isAuthenticated) {
            console.log('🍪 Token найден, редирект на Dashboard');
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err: any) => console.warn('❌ Ошибка проверки авторизации:', err)
      });
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
        console.log('✅ Login successful, redirecting...');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('🚨 Login error:', err);
        this.errorMessage = '🚨 Ошибка входа. Проверьте email и пароль.';
      },
    });
  }
}
