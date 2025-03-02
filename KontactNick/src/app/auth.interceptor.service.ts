import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';

export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // ✅ Логируем исходный запрос
  console.log("📡 Intercepting request:", req.url);

  // ✅ Создаём клон запроса с `withCredentials: true`, чтобы браузер отправлял куки автоматически
  const clonedReq = req.clone({
    withCredentials: true, // 🔥 Гарантируем отправку HttpOnly куки `jwt-token`
  });

  return next(clonedReq);
};
