import { Routes } from '@angular/router';
import { UserRegistrationComponent } from './user-registration/user-registration.component';
import { CategoryManagementComponent } from './category-management/category-management.component';
import { FieldManagementComponent } from './field-management/field-management.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AuthGuard } from './auth.guard';

export const routes: Routes = [
  { path: 'user-registration', component: UserRegistrationComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'categories', component: CategoryManagementComponent, canActivate: [AuthGuard] },
  { path: 'fields', component: FieldManagementComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/user-registration', pathMatch: 'full' },
  { path: '**', redirectTo: '/user-registration' } // Для обработки несуществующих маршрутов
];
