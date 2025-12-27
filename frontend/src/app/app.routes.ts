
import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { EmployeeListComponent } from './admin/employee-list.component';
import { PayrollEntryComponent } from './payroll/payroll-entry.component';
import { ReportComponent } from './payroll/report.component';
import { UserDashboardComponent } from './payroll/user-dashboard.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },

    // Admin Routes
    {
        path: 'admin/employees',
        component: EmployeeListComponent,
        canActivate: [authGuard],
        data: { roles: ['ADMIN'] }
    },
    {
        path: 'upload',
        loadComponent: () => import('./upload/upload.component').then(m => m.UploadComponent),
        canActivate: [authGuard],
        data: { roles: ['USER'] }
    },

    // User Routes
    {
        path: 'user/home',
        component: UserDashboardComponent,
        canActivate: [authGuard],
        data: { roles: ['USER'] }
    },
    {
        path: 'payroll/entry/:periodId',
        component: PayrollEntryComponent,
        canActivate: [authGuard],
        data: { roles: ['USER'] }
    },
    {
        path: 'reports',
        component: ReportComponent,
        canActivate: [authGuard],
        data: { roles: ['USER', 'ADMIN'] }
    },
    {
        path: 'payroll/attendance-casual/:periodId',
        loadComponent: () => import('./payroll/casual-attendance.component').then(m => m.CasualAttendanceComponent),
        canActivate: [authGuard],
        data: { roles: ['USER'] }
    },

    // Billing Routes
    {
        path: 'billing',
        loadComponent: () => import('./billing/billing-dashboard.component').then(m => m.BillingDashboardComponent),
        canActivate: [authGuard],
        data: { roles: ['BILL'] }
    },

    // Fallback
    { path: '**', redirectTo: 'login' }
];
