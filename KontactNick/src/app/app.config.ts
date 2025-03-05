import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient } from '@angular/common/http';
import {AuthService} from '@app/services/auth.service';

export const appConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    AuthService
  ],
};
