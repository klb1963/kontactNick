import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {HttpErrorResponse} from '@angular/common/http';
import {first} from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true
})
export class DashboardComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,  // ✅ Для получения параметров из URL
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.authService.checkAuth()
      .pipe(first())
      .subscribe({
        error: (err) => {
          this.hadnleCheckAuthError(err)
        }
      });
    // console.log('🟢 DashboardComponent initialized');
    // console.log('🔍 Current URL:', window.location.href);
    //
    // // ✅ Проверяем токен в localStorage перед загрузкой дашборда
    // const storedToken = localStorage.getItem('authToken');
    // console.log('📂 localStorage authToken (before dashboard):', storedToken);
    //
    // const urlParams = new URLSearchParams(window.location.search);
    // const token = urlParams.get('token');
    //
    // console.log('🔍 Token from URL:', token);
    //
    // if (token) {
    //   console.log('💾 Saving token:', token);
    //   this.authService.saveToken(token);
    //   this.clearQueryParams();
    // } else {
    //   console.warn('🚨 Dashboard: No token in URL, checking localStorage');
    // }

    // setTimeout(() => {
    //   if (!this.authService.isLoggedIn()) {
    //     console.warn('🚨 Dashboard: No valid token found, redirecting to login');
    //     this.router.navigate(['/login']);
    //   } else {
    //     console.log('✅ Dashboard: User is authenticated');
    //   }
    // }, 500);
  }

  // /** ✅ Получаем токен из URL */
  // private getTokenFromUrl(): string | null {
  //   return this.route.snapshot.queryParamMap.get('token');
  // }
  //
  // /** ✅ Очищаем параметры в URL */
  // private clearQueryParams(): void {
  //   this.router.navigate([], {
  //     queryParams: {},
  //     queryParamsHandling: 'merge'
  //   });
  // }

  logout(): void {
    this.authService.logout();
  }

  private hadnleCheckAuthError(err: HttpErrorResponse) {
    console.warn('🚨 Dashboard: No valid token found, redirecting to login');
    this.router.navigate(['/login']);
  }
}
