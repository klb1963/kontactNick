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
        console.log('🔍 Auth check result:', isAuthenticated); // ✅ Проверяем, что реально приходит
      }),
      map(isAuthenticated => {
        if (!isAuthenticated) {
          console.warn('⛔ AuthGuard: User not authenticated, redirecting to login');
          this.router.navigate(['/login']);
        } else {
          console.log('✅ AuthGuard: User is authenticated, allowing access');
        }
        return isAuthenticated;
      }),
      catchError(error => {
        console.error('🚨 AuthGuard Error:', error);
        this.router.navigate(['/login']);
        return of(false);
      })
    );
  }
}
