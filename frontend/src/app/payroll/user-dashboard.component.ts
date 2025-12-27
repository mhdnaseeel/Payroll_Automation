import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PayrollService } from './payroll.service';
import { MonthSelectionService } from '../core/services/month-selection.service';

@Component({
    selector: 'app-user-dashboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="container mt-5 fade-in">
      
      <div class="row justify-content-center">
        <div class="col-lg-8">
          
          <div class="text-center mb-4">
              <h1 class="display-4 fw-bold text-dark">Payroll Dashboard</h1>
              <p class="text-muted lead">Manage monthly payroll submissions effortlessly</p>
          </div>

          

          <!-- Main Options Cards (Visible when Form is HIDDEN) -->
          <div class="row g-4 justify-content-center" *ngIf="!showEntryForm">
             
             <!-- Card 1: New / Open Report -->
             <div class="col-md-4">
                 <div class="card h-100 shadow-sm border-0 rounded-4 action-card" (click)="toggleEntryForm('payroll')">
                    <div class="card-body p-4 text-center">
                        <div class="icon-circle mb-3 bg-success-subtle text-success">
                            <i class="bi bi-file-earmark-plus"></i>
                        </div>
                        <h4 class="fw-bold">File New Report</h4>
                        <p class="text-muted small">Start new monthly payroll entry.</p>
                        <button class="btn btn-outline-success rounded-pill px-4 mt-2 w-100">Get Started</button>
                    </div>
                 </div>
             </div>

             
             <!-- Card 2: Casual Attendance -->
             <div class="col-md-4">
                 <div class="card h-100 shadow-sm border-0 rounded-4 action-card" (click)="toggleEntryForm('casual')">
                    <div class="card-body p-4 text-center">
                        <div class="icon-circle mb-3 bg-warning-subtle text-warning">
                            <i class="bi bi-calendar-check"></i>
                        </div>
                        <h4 class="fw-bold">Casual Attendance</h4>
                        <p class="text-muted small">Mark daily attendance for casual labourers.</p>
                        <button class="btn btn-outline-warning rounded-pill px-4 mt-2 w-100 text-dark">Mark Attendance</button>
                    </div>
                 </div>
             </div>

             <!-- Card 3: History -->
             <div class="col-md-4">
                 <div class="card h-100 shadow-sm border-0 rounded-4 action-card" (click)="viewReports()">
                    <div class="card-body p-4 text-center">
                        <div class="icon-circle mb-3 bg-primary-subtle text-primary">
                            <i class="bi bi-clock-history"></i>
                        </div>
                        <h4 class="fw-bold">History</h4>
                        <p class="text-muted small">View past submissions and reports.</p>
                        <button class="btn btn-outline-primary rounded-pill px-4 mt-2 w-100">View Records</button>
                    </div>
                 </div>
             </div>

             
          </div>

          <!-- Entry Form (Visible when TOGGLED) -->
          <div *ngIf="showEntryForm" class="card shadow-lg border-0 rounded-4 fade-in">
              <div class="card-body p-5">
                  <div class="d-flex justify-content-between align-items-center mb-4">
                      <h3 class="fw-bold mb-0">
                          {{ targetMode === 'casual' ? 'Casual Attendance Entry' : 'New Monthly Payroll' }}
                      </h3>
                      <button class="btn btn-close" (click)="toggleEntryForm()"></button>
                  </div>
                  
                  <div class="alert border-0 mb-4" [ngClass]="targetMode === 'casual' ? 'alert-warning bg-warning-subtle text-warning-emphasis' : 'alert-info bg-info-subtle text-info-emphasis'">
                      <i class="bi bi-info-circle-fill me-2"></i>
                      Select Period to {{ targetMode === 'casual' ? 'Mark Attendance' : 'File Report' }}.
                  </div>

                  <div class="row g-3">
                      <div class="col-md-6">
                          <label class="form-label fw-bold">Select Month</label>
                          <select class="form-select" [(ngModel)]="selectedMonth" (ngModelChange)="onSelectionChange()">
                              <option *ngFor="let m of months; let i = index" [value]="i+1">{{ m }}</option>
                          </select>
                      </div>
                      <div class="col-md-6">
                          <label class="form-label fw-bold">Select Year</label>
                          <input type="number" class="form-control" [(ngModel)]="selectedYear" (ngModelChange)="onSelectionChange()">
                      </div>
                      <div class="col-12">
                           <label class="form-label fw-bold">Last Working Day (Date)</label>
                           <input type="date" class="form-control" [(ngModel)]="lastWorkingDay">
                      </div>
                  </div>

                  <div *ngIf="errorMessage" class="alert alert-danger mt-3">
                      {{ errorMessage }}
                  </div>

                  <div class="d-grid gap-2 mt-4">
                      <button class="btn btn-lg rounded-pill fw-bold" 
                              [ngClass]="targetMode === 'casual' ? 'btn-warning text-dark' : 'btn-primary'"
                              (click)="startEntry()" [disabled]="isLoading">
                          <span *ngIf="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                          {{ isLoading ? 'Processing...' : (targetMode === 'casual' ? 'Open Attendance' : 'Create / Open Entry') }}
                      </button>
                  </div>
              </div>
          </div>

        </div>
      </div>
    </div>

    <style>
      .icon-circle {
          width: 80px; height: 80px;
          border-radius: 50%;
          display: inline-flex;
          align-items: center; justify-content: center;
          font-size: 2.5rem;
      }
      .action-card {
          transition: transform 0.2s, box-shadow 0.2s;
          cursor: pointer;
      }
      .action-card:hover {
          transform: translateY(-5px);
          box-shadow: 0 10px 20px rgba(0,0,0,0.1) !important;
      }
      .fade-in {
          animation: fadeIn 0.3s ease-in;
      }
      @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    </style>
  `
})
export class UserDashboardComponent {
    months = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];


    // For New Entry
    selectedMonth: number = new Date().getMonth() + 1;
    selectedYear: number = new Date().getFullYear();
    lastWorkingDay: string = new Date().toISOString().split('T')[0];

    showEntryForm = false;
    targetMode: 'payroll' | 'casual' = 'payroll';
    isLoading = false;
    errorMessage: string | null = null;

    // Subscription for global selection
    private monthSelectionSub: any;

    constructor(
        private http: HttpClient,
        private router: Router,
        private payrollService: PayrollService,
        private monthService: MonthSelectionService
    ) { }

    // Cache existing periods
    existingPeriods: any[] = [];

    ngOnInit() {
        this.fetchPeriods();
    }

    fetchPeriods() {
        this.payrollService.getPeriods().subscribe({
            next: (data) => {
                this.existingPeriods = data;
                this.checkAndPopulateDate();
            },
            error: (e) => console.error('Failed to fetch existing periods', e)
        });
    }

    checkAndPopulateDate() {
        const found = this.existingPeriods.find(p => p.month == this.selectedMonth && p.year == this.selectedYear);
        if (found && found.lastWorkingDay) {
            this.lastWorkingDay = found.lastWorkingDay;
        } else {
            // Optional: Reset to today if not found? 
            // this.lastWorkingDay = new Date().toISOString().split('T')[0];
            // Better to leave it distinct so user knows it's default
        }
    }

    onSelectionChange() {
        this.checkAndPopulateDate();
    }

    ngOnDestroy() {
        if (this.monthSelectionSub) {
            this.monthSelectionSub.unsubscribe();
        }
    }

    toggleEntryForm(mode: 'payroll' | 'casual' = 'payroll') {
        this.targetMode = mode;
        this.showEntryForm = !this.showEntryForm;
        this.errorMessage = null;
    }



    viewReports() {
        this.router.navigate(['/reports']);
    }

    startEntry() {
        this.isLoading = true;
        this.errorMessage = null;

        const payload = {
            month: this.selectedMonth,
            year: this.selectedYear,
            lastWorkingDay: this.lastWorkingDay
        };

        this.payrollService.createPeriod(payload).subscribe({
            next: (period) => {
                if (this.targetMode === 'casual') {
                    this.router.navigate(['/payroll/attendance-casual', period.id]);
                } else {
                    this.router.navigate(['/payroll/entry', period.id]);
                }
            },
            error: (err) => {
                console.error(err);
                this.isLoading = false;
                if (err.status === 409) {
                    // Period exists. Find it.
                    // Force refresh to ensure we have latest data
                    this.payrollService.refreshPeriods();
                    this.errorMessage = "Period exists. Finding it...";
                    this.payrollService.getPeriods().subscribe(periods => {
                        const match = periods.find(p => p.month == this.selectedMonth && p.year == this.selectedYear);
                        if (match) {
                            if (this.targetMode === 'casual') {
                                this.router.navigate(['/payroll/attendance-casual', match.id]);
                            } else {
                                this.router.navigate(['/payroll/entry', match.id]);
                            }
                        } else {
                            this.errorMessage = "Period exists but could not be found. Please check History.";
                        }
                    });
                } else {
                    this.errorMessage = err.error?.message || err.message || 'Failed to start period.';
                }
            }
        });
    }
}
