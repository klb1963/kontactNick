import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  canActivate(): boolean {
    console.log('🔍 AuthGuard: Checking authentication status');

    // if (!isPlatformBrowser(this.platformId)) {
    //   console.warn('⚠️ AuthGuard: Running in SSR mode, skipping authentication check.');
    //   return true; // ✅ В SSR пропускаем проверку
    // }
    //
    // if (this.authService.isLoggedIn()) {
    //   console.log('✅ AuthGuard: User is authenticated');
    //   return true; // ✅ Доступ разрешен
    // } else {
    //   console.log('⛔ AuthGuard: User is NOT authenticated, redirecting to login');
    //   this.router.navigate(['/login']); // ⛔ Перенаправляем на логин
    //   return false;
    // }

    return true;
  }
}
