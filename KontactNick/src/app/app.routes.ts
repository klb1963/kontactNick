import { Routes } from '@angular/router';
import { UserRegistrationComponent } from './user-registration/user-registration.component';
import { CategoryManagementComponent } from './category-management/category-management.component';
import { FieldManagementComponent } from './field-management/field-management.component';

export const routes: Routes = [
  { path: '', redirectTo: '/register', pathMatch: 'full' },
  { path: 'register', component: UserRegistrationComponent },
  { path: 'categories', component: CategoryManagementComponent },
  { path: 'fields', component: FieldManagementComponent }
];
