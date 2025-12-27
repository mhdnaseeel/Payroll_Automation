
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth/auth.service';
import { PayrollService } from './payroll/payroll.service';
import { MonthSelectionService } from './core/services/month-selection.service';

import { ConfirmDialogComponent } from './core/components/confirm-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, FormsModule, ConfirmDialogComponent],
  template: `
    <app-confirm-dialog></app-confirm-dialog>
    <div class="d-flex h-100" *ngIf="authService.userRole$ | async as role; else loginView">
      
      <!-- Overlay (Mobile) -->
      <div *ngIf="isSidebarOpen" class="sidebar-overlay" (click)="toggleSidebar()"></div>

      <!-- Sidebar (Drawer) -->
      <div class="sidebar text-white p-3 vh-100 shadow" 
           [class.open]="isSidebarOpen">
         <!-- Close Button (Mobile/Drawer) -->
         <div class="d-flex justify-content-between align-items-center mb-4">
            <h4 class="mb-0">FCI Payroll</h4>
            <button class="btn btn-sm btn-outline-light border-0" (click)="toggleSidebar()">
                <i class="bi bi-x-lg"></i>
            </button>
         </div>

        <div class="mb-3">
            <small class="text-white-50">User: {{ role }}</small>
        </div>
        
        <ul class="nav flex-column gap-2">
            <!-- Admin Links -->
            <li class="nav-item" *ngIf="role === 'ADMIN'">
                <a routerLink="/admin/employees" class="nav-link text-white btn text-start border-0" 
                   (click)="toggleSidebar()">
                    <i class="bi bi-people me-2"></i> Employees
                </a>
            </li>

            <!-- User Links (Payroll & Reports) -->
            <ng-container *ngIf="role === 'USER'">
                <li class="nav-item">
                    <a routerLink="/user/home" class="nav-link text-white btn text-start border-0"
                       (click)="toggleSidebar()">
                        <i class="bi bi-house-door me-2"></i> Dashboard
                    </a>
                </li>

                <li class="nav-item" *ngIf="currentPeriodId">
                    <a [routerLink]="['/payroll/attendance-casual', currentPeriodId]" 
                       class="nav-link text-white btn text-start border-0 mt-2"
                       (click)="toggleSidebar()">
                        <i class="bi bi-calendar-check me-2"></i> Casual Attendance
                    </a>
                </li>
                
                <li class="nav-item">
                    <a routerLink="/reports" 
                       class="nav-link text-white btn text-start border-0 mt-2"
                       [ngClass]="{'disabled': !isReportsEnabled()}"
                       (click)="!isReportsEnabled() ? null : toggleSidebar()">
                        <i class="bi bi-file-earmark-text me-2"></i> Reports
                        <span *ngIf="!isReportsEnabled()" class="badge ms-2 bg-secondary">Locked</span>
                    </a>
                </li>

                <li class="nav-item">
                    <a routerLink="/upload" class="nav-link text-white btn text-start border-0 mt-2"
                       (click)="toggleSidebar()">
                        <i class="bi bi-cloud-upload me-2"></i> Upload
                    </a>
                </li>
            </ng-container>

            <!-- Billing Links (For BILL Role) -->
            <ng-container *ngIf="role === 'BILL'">
               <li class="nav-item">
                    <a routerLink="/billing" [queryParams]="{module: 'DASHBOARD'}" class="nav-link text-white btn text-start border-0 fw-bold"
                       (click)="toggleSidebar()">
                        <i class="bi bi-grid-1x2 me-2"></i> Dashboard
                    </a>
                </li>
                 <li class="nav-item">
                    <a routerLink="/billing" [queryParams]="{module: 'RECEIPT'}" class="nav-link text-white btn text-start border-0 mt-2 w-100"
                       (click)="toggleSidebar()">
                        <i class="bi bi-receipt me-2"></i> Receipt
                    </a>
                </li>
                <li class="nav-item">
                    <a routerLink="/billing" [queryParams]="{module: 'ISSUE'}" class="nav-link text-white btn text-start border-0 mt-2 w-100"
                       (click)="toggleSidebar()">
                        <i class="bi bi-box-seam me-2"></i> Issue
                    </a>
                </li>
                 <li class="nav-item">
                    <a routerLink="/billing" [queryParams]="{module: 'QC'}" class="nav-link text-white btn text-start border-0 mt-2 w-100"
                       (click)="toggleSidebar()">
                        <i class="bi bi-person-gear me-2"></i> QC Module
                    </a>
                </li>
                 <li class="nav-item">
                    <a routerLink="/billing" [queryParams]="{module: 'BILL'}" class="nav-link text-white btn text-start border-0 mt-2 w-100"
                       (click)="toggleSidebar()">
                        <i class="bi bi-file-pdf me-2"></i> Bill Generator
                    </a>
                </li>
            </ng-container>
        </ul>
        
        <hr class="mt-auto border-secondary">
        
        <!-- Role Switcher -->
        <div class="mb-2" *ngIf="canSwitchToUser()">
            <button class="btn w-100 text-white shadow-sm" style="background-color: #2c5f2d; font-size: 0.9rem;" (click)="switchRole('USER'); toggleSidebar()">
                <i class="bi bi-person-badge"></i> View as User
            </button>
        </div>

        <button class="btn btn-danger w-100" (click)="logout()">Logout</button>
      </div>

      <!-- Main Content -->
      <div class="flex-grow-1 h-100 overflow-auto">
         <nav class="navbar navbar-expand-lg navbar-light bg-white shadow-sm mb-4 px-4 sticky-top">
            <button class="btn btn-link text-dark me-3 p-0" (click)="toggleSidebar()" title="Menu">
                <i class="bi bi-list fs-3"></i>
            </button>
            <span class="navbar-brand h1 mb-0">Dashboard</span>
         </nav>
         <div class="px-4 pb-5">
             <router-outlet></router-outlet>
         </div>
      </div>
    </div>

    <!-- Login View (When no role) -->
    <ng-template #loginView>
        <router-outlet></router-outlet>
    </ng-template>

    <style>
      .sidebar {
          width: 250px;
          position: fixed;
          top: 0;
          left: -260px; /* Hidden by default */
          background-color: #0f2615;
          transition: left 0.3s ease-in-out;
          z-index: 1050;
      }
      .sidebar.open {
          left: 0;
      }
      .sidebar-overlay {
          position: fixed;
          top: 0; left: 0; right: 0; bottom: 0;
          background: rgba(0,0,0,0.5);
          z-index: 1040;
      }
    </style>
  `
})
export class AppComponent {
  constructor(
    public authService: AuthService,
    public payrollService: PayrollService,
    private router: Router,
    private monthService: MonthSelectionService
  ) {
    const current = this.monthService.getSelection();
    this.selectedMonth = current.month;
    this.selectedYear = current.year;
  }

  goHome() {
    this.toggleSidebar();
  }

  isSidebarOpen = false;

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  currentPeriodStatus: 'OPEN' | 'CLOSED' | undefined;
  currentPeriodId: string | null = null;

  private pollInterval: any;

  ngOnInit() {
    // Subscribe to Role changes to Start/Stop Heartbeat
    this.authService.userRole$.subscribe(role => {
      if (role) {
        this.startPolling();
      } else {
        this.stopPolling();
      }
    });

    // Subscribe to period status to toggle Reports
    this.payrollService.currentPeriod$.subscribe(p => {
      if (p) {
        this.currentPeriodStatus = p.status;
        this.currentPeriodId = p.id;
      } else {
        this.currentPeriodId = null;
      }
    });
  }

  startPolling() {
    // Avoid multiple intervals
    if (this.pollInterval) return;

    // Immediate check on start (handles page refresh)
    if (this.authService.getRole()) {
      this.authService.checkServerSession();
    }

    this.pollInterval = setInterval(() => {
      if (this.authService.getRole()) {
        this.authService.checkServerSession();
      }
    }, 3000);
  }

  stopPolling() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  isReportsEnabled(): boolean {
    // Admin always sees reports? Or also subject to lock? 
    // User request: "after final submission only shows"
    const role = this.authService.getRole();
    if (role === 'ADMIN') return true;

    return this.currentPeriodStatus === 'CLOSED';
  }

  logout() {
    this.authService.logout();
    window.location.reload();
  }

  // Role Switching Logic
  canSwitchToUser(): boolean {
    return this.authService.getRole() === 'ADMIN';
  }

  switchRole(target: string) {
    this.authService.switchRole(target);
    // Redirect to appropriate landing page
    if (target === 'ADMIN') {
      window.location.href = '/admin/employees';
    } else {
      window.location.href = '/user/home';
    }
  }

  // Month Selection Logic
  months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  selectedMonth: number = new Date().getMonth() + 1;
  selectedYear: number = new Date().getFullYear();

  updateGlobalSelection() {
    this.monthService.setSelection(this.selectedMonth, this.selectedYear);
  }
}
