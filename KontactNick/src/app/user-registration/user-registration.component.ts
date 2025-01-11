import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../auth.service';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-user-registration',
  templateUrl: './user-registration.component.html',
  styleUrls: ['./user-registration.component.css'],
  standalone: true,
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
  ],
})

export class UserRegistrationComponent {
  email = '';
  password = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  register(): void {
    this.authService.register(this.email, this.password).subscribe({
      next: () => {
        console.log('Registration successful');
        this.router.navigate(['/login']); // Перенаправляем на страницу логина
      },
      error: (err) => {
        console.error('Registration error:', err);
        this.errorMessage = 'Registration failed. Please try again.';
      },
    });
  }

  login(): void {
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        console.log('Login successful:', response);
        this.authService.saveToken(response.token);
        this.router.navigate(['/dashboard']); // Перенаправляем в личный кабинет
      },
      error: (err) => {
        console.error('Login error:', err);
        this.errorMessage = 'Login failed. Please try again.';
      },
    });

// это от аутентификации через Google
// export class UserRegistrationComponent {
//   constructor(private router: Router, private authService: AuthService) {
//   }
//
//   ngOnInit(): void {
//     this.authService.getTokenFromServer().subscribe({
//       next: (token) => {
//         console.log('Token received:', token); // Для отладки
//         this.authService.saveToken(token); // Сохраняем токен
//         this.router.navigate(['/dashboard']); // Перенаправляем пользователя
//       },
//       error: (err) => {
//         console.error('Error fetching token:', err);
//       },
//     });
//   }
//
//   register(email: string): void {
//     console.log(`Registering with email: ${email}`);
//   }
//
//   googleLogin(): void {
//     window.location.href = 'http://localhost:8080/oauth2/authorization/google';

  }
}
