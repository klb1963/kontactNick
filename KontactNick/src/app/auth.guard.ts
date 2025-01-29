import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { isPlatformBrowser } from '@angular/common';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';

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
    console.log('🔍 AuthGuard: Checking authentication status');

    if (!isPlatformBrowser(this.platformId)) {
      console.warn('⚠️ AuthGuard: Running in SSR mode, skipping authentication check.');
      return new Observable<boolean>(observer => {
        observer.next(true);
        observer.complete();
      }); // ✅ В SSR пропускаем проверку
    }

    return this.authService.checkAuthStatus().pipe(
      tap(isAuthenticated => {
        if (isAuthenticated) {
          console.log('✅ AuthGuard: User is authenticated');
        } else {
          console.log('⛔ AuthGuard: User is NOT authenticated, redirecting to login');
          this.router.navigate(['/login']);
        }
      }),
      map(isAuthenticated => isAuthenticated)
    );
  }
}
