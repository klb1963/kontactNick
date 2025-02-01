import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean> {
    console.log('🔍 AuthGuard: Checking authentication status');

    return this.authService.checkAuthStatus().pipe(
      tap(isAuthenticated => {
        if (isAuthenticated) {
          console.log('✅ AuthGuard: User is authenticated');
        } else {
          console.log('⛔ AuthGuard: User is NOT authenticated, redirecting to login');
          this.router.navigate(['/login']);
        }
      }),
      map(isAuthenticated => isAuthenticated),
      catchError((error) => {
        console.error('🚨 AuthGuard Error:', error);
        this.router.navigate(['/login']);
        return of(false);
      })
    );
  }
}
