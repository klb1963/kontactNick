import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { isPlatformBrowser } from '@angular/common';
import { Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  canActivate(): Observable<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return of(true); // ✅ Пропускаем проверку для SSR
    }

    return this.authService.checkAuthStatus().pipe(
      tap(isAuthenticated => {
        if (isAuthenticated) {
          console.log('✅ AuthGuard: User is authenticated');
        } else {
          if (window.location.pathname !== '/login') {
            console.warn('⛔ AuthGuard: Redirecting to login because user is not authenticated');
          }
          this.router.navigate(['/login']);
        }
      }),
      map(isAuthenticated => isAuthenticated),
      catchError(error => {
        if (error.status !== 401) { // ✅ Показываем только важные ошибки
          console.error('🚨 AuthGuard Error:', error);
        }
        this.router.navigate(['/login']);
        return of(false);
      })
    );
  }
}
