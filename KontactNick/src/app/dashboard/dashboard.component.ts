import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true
})
export class DashboardComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.handleGoogleLogin(); // ✅ Проверяем токен из URL и сохраняем его
    this.checkAuthentication(); // ✅ Проверяем, есть ли сохраненный токен
  }

  /** ✅ Проверка, залогинен ли пользователь */
  private checkAuthentication(): void {
    setTimeout(() => {
      if (!this.authService.isLoggedIn()) {
        console.warn('🚨 Dashboard: No valid token found, redirecting to login');
        this.router.navigate(['/login']);
      }
    }, 500);
  }

  logout(): void {
    this.authService.logout();
  }
}
