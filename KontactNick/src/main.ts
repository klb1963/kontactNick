import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { AppComponent } from './app/app.component';
import { AuthInterceptor } from './app/auth.interceptor.service';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'; // ✅ Импортируем как ФУНКЦИЮ

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([AuthInterceptor])), provideAnimationsAsync() // ✅ Теперь передаем функцию
  ],
}).catch((err) => console.error(err));
