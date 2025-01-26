import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { first } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true
})
export class DashboardComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('🔍 DashboardComponent initialized, checking authentication...');
    this.authService.checkAuth()
      .pipe(first())
      .subscribe({
        next: (isAuthenticated: boolean) => {
          if (isAuthenticated) {
            console.log('✅ User is authenticated, fetching token...');
            this.fetchToken();
          } else {
            console.warn('❌ User is NOT authenticated, redirecting to login');
            this.router.navigate(['/login']);
          }
        },
        error: () => {
          console.warn('🚨 Authentication check failed, redirecting to login');
          this.router.navigate(['/login']);
        }
      });
  }

  private fetchToken(): void {
    const jwtToken = this.authService.getToken();

    if (!jwtToken) {
      console.warn('❌ No JWT found, skipping /api/auth/token request');
      return;
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);

    this.http.get('/api/auth/token', { headers, withCredentials: true }).subscribe({
      next: (data) => console.log('✅ Token received:', data),
      error: (err: any) => console.error('❌ Error fetching token:', err)
    });
  }

  logout(): void {
    console.log('🔴 Logging out...');
    this.authService.logout();
  }
}
