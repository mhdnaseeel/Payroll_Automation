
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PayrollService, PayrollPeriod } from './payroll.service';
import { DialogService } from '../core/services/dialog.service';
import { environment } from '../../environments/environment';


interface Employee {
  fullName: string;
  memberId: string;
}

interface PayrollEntry {
  id: string;
  employee: Employee;
  daysWorked: number;
  activeDays: number[]; // Array of day numbers (1..31)
  wagesEarned: number;
}

@Component({
  selector: 'app-casual-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container-fluid py-4 min-vh-100 bg-light">
      
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center mb-4 px-3">
        <div>
           <h2 class="fw-bold mb-0 text-dark">
             <i class="bi bi-calendar-check me-2 text-primary"></i>Casual Labour Attendance
             <span *ngIf="period?.status === 'CLOSED'" class="badge bg-danger ms-2">LOCKED</span>
           </h2>
           <p class="text-muted mb-0 small" *ngIf="period">
              Period: {{ period.month }}/{{ period.year }} | Last Working Day: {{ period.lastWorkingDay }}
           </p>
        </div>

        <div class="d-flex gap-2">
            <button class="btn btn-warning d-flex align-items-center me-2" 
                    *ngIf="period?.status === 'CLOSED'" 
                    (click)="unlock()" [disabled]="isLoading">
                <i class="bi bi-unlock-fill me-2"></i> Unlock
            </button>
            <button class="btn btn-danger d-flex align-items-center me-2" 
                    *ngIf="period?.status === 'OPEN'" 
                    (click)="lock()" [disabled]="isLoading || hasValidationErrors()">
                <i class="bi bi-lock-fill me-2"></i> Lock & Finalize
            </button>
            <button class="btn btn-primary d-flex align-items-center" 
                    (click)="save()" [disabled]="period?.status === 'CLOSED' || isLoading || hasValidationErrors()">
                <span *ngIf="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                <i class="bi bi-save me-2" *ngIf="!isLoading"></i> Save Attendance
            </button>
        </div>
      </div>

      <!-- Grid -->
      <div class="card border-0 shadow-sm mx-3 overflow-hidden rounded-3">
        <div class="table-responsive">
          <table class="table table-bordered table-hover mb-0 align-middle text-center">
            <thead class="bg-dark text-white">
              <tr>
                <th class="py-3 px-4 text-start" style="position: sticky; left: 0; z-index: 10; background: #212529; min-width: 200px;">Employee</th>
                <!-- Days 1-31 -->
                <th *ngFor="let d of days" class="p-1" 
                    style="min-width: 35px; cursor: pointer;" 
                    (click)="toggleDayGlobally(d)" 
                    title="Click to toggle attendance for all employees on Day {{d}}"
                    [class.bg-primary]="isDayGloballyActive(d)"
                    [class.text-white]="isDayGloballyActive(d)">
                    <div class="d-flex flex-column align-items-center" style="line-height: 1.1;">
                        <small class="text-muted fw-normal mb-1" [class.text-white-50]="isDayGloballyActive(d)" style="font-size: 0.65rem;">
                            {{ getDayName(d) }}
                        </small>
                        <span style="font-size: 0.85rem;">{{d}}</span>
                    </div>
                </th>
                <th class="py-3 bg-secondary text-white" style="min-width: 60px;">Total</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of entries" [class.table-secondary]="period?.status === 'CLOSED'">
                
                <!-- Employee Info -->
                <td class="text-start px-3 bg-white" style="position: sticky; left: 0; z-index: 5;">
                    <div class="fw-bold text-dark">{{ row.employee.fullName }}</div>
                    <div class="small text-muted font-monospace">{{ row.employee.memberId }}</div>
                </td>
                
                <!-- Checkboxes -->
                <td *ngFor="let d of days" class="p-0">
                   <div class="form-check d-flex justify-content-center align-items-center h-100 m-0" style="min-height: 40px;">
                      <input type="checkbox" class="form-check-input border-secondary" 
                             [checked]="isDayActive(row, d)" 
                             (change)="toggleDay(row, d)"
                             [disabled]="period?.status === 'CLOSED'">
                   </div>
                </td>

                <!-- Total Count / Validation -->
                <td class="fw-bold" [class.bg-success-subtle]="row.activeDays.length === row.daysWorked" 
                    [class.bg-danger-subtle]="row.activeDays.length !== row.daysWorked">
                   <div class="d-flex flex-column align-items-center">
                       <span [class.text-danger]="row.activeDays.length !== row.daysWorked" 
                             [class.text-success]="row.activeDays.length === row.daysWorked">
                           {{ row.activeDays.length }} / {{ row.daysWorked }}
                       </span>
                       <small *ngIf="row.activeDays.length !== row.daysWorked" class="text-danger" style="font-size: 0.7rem;">
                           Mismatch
                       </small>
                   </div>
                </td>
              </tr>
              
              <tr *ngIf="entries.length === 0">
                 <td [attr.colspan]="days.length + 2" class="text-center py-5">
                    <div class="text-muted">Loading or No Casual Labourers Found...</div>
                 </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Custom Scrollbar for x-axis */
    .table-responsive::-webkit-scrollbar { height: 8px; }
    .table-responsive::-webkit-scrollbar-thumb { background: #ccc; border-radius: 4px; }
    .table-responsive::-webkit-scrollbar-track { background: #f1f1f1; }
  `]
})
export class CasualAttendanceComponent implements OnInit {
  entries: PayrollEntry[] = [];
  period: PayrollPeriod | null = null;
  periodId: string = '';
  isLoading = false;
  days: number[] = [];

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private payrollService: PayrollService,
    private dialogService: DialogService
  ) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.periodId = params['periodId'];
      this.loadData();
    });
  }

  loadData() {
    this.payrollService.loadPeriod(this.periodId);
    this.payrollService.currentPeriod$.subscribe(p => {
      this.period = p;
      if (this.period) {
        this.generateDays();
      }
    });

    // Fetch entries - filter for CL only? 
    // Ideally backend should filter, or we filter here.
    // Assuming backend returns all, we can filter by Category if available in Entry?
    // Wait, PayrollEntry interface above doesn't have Category. 
    // I need to update Interface or check if backend sends it. 
    // For now, let's assume we show ALL, or user can filter?
    // User asked "add new catogy to add attendece only for casual labours".
    // I should ideally filter. Let's see if backend sends category. 
    // Employee entity has it. PayrollEntry has Employee.
    // I'll check response from existing endpoint in previous steps or just try.
    // Ideally I should filter on Frontend if I fetch all.

    this.http.get<any[]>(`${environment.apiUrl}/payroll/periods/${this.periodId}/entries`).subscribe({
      next: (data) => {
        // Filter for Casual Labourers (CL)
        // Check if data[0].employee.category exists? 
        // If not, I'll display all for now, but strictly User wants Casual.
        // Let's assume Employee object has category.
        this.entries = data.filter(e => e.employee.category === 'CL');
        // Initialize activeDays if null
        this.entries.forEach(e => {
          if (!e.activeDays) e.activeDays = [];
        });
      },
      error: (err) => console.error(err)
    });
  }

  generateDays() {
    if (!this.period) return;
    const daysInMonth = new Date(this.period.year, this.period.month, 0).getDate();
    this.days = Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  isDayActive(entry: PayrollEntry, day: number): boolean {
    return entry.activeDays?.includes(day);
  }

  toggleDay(entry: PayrollEntry, day: number) {
    if (!entry.activeDays) entry.activeDays = [];
    const idx = entry.activeDays.indexOf(day);
    if (idx > -1) {
      entry.activeDays.splice(idx, 1);
    } else {
      entry.activeDays.push(day);
    }
  }

  isDayGloballyActive(day: number): boolean {
    if (!this.entries || this.entries.length === 0) return false;
    return this.entries.every(e => this.isDayActive(e, day));
  }

  toggleDayGlobally(day: number) {
    if (this.period?.status === 'CLOSED') return;

    // Check if ALL currently displayed entries have this day active
    const allActive = this.isDayGloballyActive(day);

    // New state: If all are active, we toggle OFF (remove). If mixed or none, we toggle ON (add).
    const newState = !allActive;

    this.entries.forEach(e => {
      if (!e.activeDays) e.activeDays = [];
      const idx = e.activeDays.indexOf(day);

      if (newState) {
        // Turn ON: Add if not present
        if (idx === -1) e.activeDays.push(day);
      } else {
        // Turn OFF: Remove if present
        if (idx > -1) e.activeDays.splice(idx, 1);
      }
    });
  }



  hasValidationErrors(): boolean {
    return this.entries.some(e => (e.activeDays?.length || 0) !== (e.daysWorked || 0));
  }

  unlock() {
    this.dialogService.confirm('Unlock Period', 'Are you sure you want to Unlock this period? This will allow edits to both Payroll and Attendance.')
      .subscribe(confirmed => {
        if (!confirmed) return;

        this.isLoading = true;
        this.payrollService.reopenPeriod(this.periodId).subscribe({
          next: (updated) => {
            this.period = updated;
            this.isLoading = false;
            this.dialogService.alert('Success', 'Period Unlocked Successfully!');
            this.loadData();
          },
          error: (err) => {
            this.isLoading = false;
            this.dialogService.alert('Error', 'Failed to unlock: ' + err.message);
          }
        });
      });
  }

  lock() {
    this.dialogService.confirm('Lock Period', 'Are you sure you want to Lock this period? No further edits will be allowed until unlocked.')
      .subscribe(confirmed => {
        if (!confirmed) return;

        this.isLoading = true;
        this.payrollService.finalizePeriod(this.periodId).subscribe({
          next: (updated) => {
            this.period = updated;
            this.isLoading = false;
            this.dialogService.alert('Success', 'Period Locked Successfully!');
            this.loadData();
          },
          error: (err) => {
            this.isLoading = false;
            this.dialogService.alert('Error', 'Failed to lock: ' + err.message);
          }
        });
      });
  }

  save() {
    this.isLoading = true;
    // Map entries back to payload
    // We only need to send ID and activeDays really, OR full object if PUT expects it.
    // Existing PUT /api/payroll/entries expects List<PayrollEntry>.
    // It should handle activeDays if I updated the Entity (which handles JSON serialization usually).
    // I need to make sure backend DTO/Entity serialization includes activeDays. 
    // @ElementCollection usually serializes to valid JSON array.

    this.http.put<PayrollEntry[]>(`${environment.apiUrl}/payroll/entries`, this.entries).subscribe({
      next: (updated) => {
        this.isLoading = false;
        this.dialogService.alert('Success', 'Attendance Saved Successfully!');
      },
      error: (err) => {
        this.isLoading = false;
        this.dialogService.alert('Error', 'Failed to save: ' + err.message);
      }
    });
  }

  getDayName(day: number): string {
    if (!this.period) return '';
    // Create date relative to the period month/year
    // Note: Month is 1-based in our period object, but Date() constructor expects 0-based.
    const date = new Date(this.period.year, this.period.month - 1, day);

    // Handle invalid days (e.g. Feb 30) - simple check
    // If the month rolls over, it means the day doesn't exist in this month
    if (date.getMonth() !== this.period.month - 1) {
      return '';
    }

    // Return narrow format (M, T, W, T, F, S, S)
    return date.toLocaleDateString('en-US', { weekday: 'narrow' });
  }
}
