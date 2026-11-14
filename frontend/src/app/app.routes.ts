import { Routes } from '@angular/router';

import { EbookCatalogueComponent } from './features/catalogue/ebook-catalogue.component';
import { EbookDetailsComponent } from './features/ebook-details/ebook-details';
import { RegisterComponent } from './features/auth/register/register.component';
import { LoginComponent } from './features/auth/login/login.component';
import { AccountComponent } from './features/account/account.component';

import { AdminLayoutComponent } from './features/admin/layout/admin-layout.component';
import { AdminDashboardComponent } from './features/admin/dashboard/admin-dashboard.component';
import { AdminEbooksComponent } from './features/admin/ebooks/admin-ebooks.component';
import { CreateEbookComponent } from './features/admin/ebooks/create/create-ebook.component';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { CategoryManagementComponent } from './features/admin/categories/category-management.component';
import { CustomerOrdersComponent } from './features/account/orders/customer-orders.component';
import { AdminPaymentsComponent } from './features/payments/admin-payments.component';
import { CartComponent } from './features/cart/cart.component';
import { MyLibraryComponent } from './features/account/library/my-library.component';

export const routes: Routes = [
  {
    path: '',
    component: EbookCatalogueComponent,
  },
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'books/:slug',
    component: EbookDetailsComponent,
  },
  {
    path: 'account',
    component: AccountComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CUSTOMER', 'ADMIN'] },
  },
  {
    path: 'account/orders',
    component: CustomerOrdersComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CUSTOMER', 'ADMIN'] },
  },
  {
    path: 'account/library',
    component: MyLibraryComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CUSTOMER'] },
  },
  { path: 'cart', component: CartComponent },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        component: AdminDashboardComponent,
      },
      {
        path: 'ebooks',
        component: AdminEbooksComponent,
      },
      {
        path: 'ebooks/create',
        component: CreateEbookComponent,
      },
      {
        path: 'categories',
        component: CategoryManagementComponent,
      },
      {
        path: 'payments',
        component: AdminPaymentsComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
