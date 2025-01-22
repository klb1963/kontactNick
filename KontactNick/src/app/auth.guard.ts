import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    // console.log('AuthGuard: Checking authentication status'); // ✅ Логируем вызов Guard
    //
    // if (this.authService.isLoggedIn()) {
    //   console.log('AuthGuard: User is authenticated'); // ✅ Пользователь залогинен
    //   return true; // ✅ Доступ разрешен
    // } else {
    //   console.log('AuthGuard: User is NOT authenticated, redirecting to login'); // ⛔ Пользователь не залогинен
    //   this.router.navigate(['/login']); // ⛔ Перенаправляем на логин
    //   return false;
    // }
    return true;
  }
}
