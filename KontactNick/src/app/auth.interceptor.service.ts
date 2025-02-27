import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';

export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);


  // ✅ Получаем токен из cookies
  const token = getCookie('jwt-token');
  console.log('🔍 Token from cookies:', token); // ⬅️ Добавляем лог

  if (token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`, // ✅ Добавляем заголовок
      },
      withCredentials: true, // ✅ Обязательно для отправки куки
    });
    return next(clonedReq);
  }

  return next(req);
};

// ✅ Функция для чтения cookies
function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? match[2] : null;
}
