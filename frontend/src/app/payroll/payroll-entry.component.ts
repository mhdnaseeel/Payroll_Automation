import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PayrollService, PayrollPeriod } from './payroll.service';
import { DialogService } from '../core/services/dialog.service';
import { environment } from '../../environments/environment';


interface PayrollEntry {
  id: string;
  employee: {
    fullName: string;
    memberId: string;
  };
  daysWorked: number;
  wagesEarned: number;
  advanceDeduction: number;
  // Calculated
  epfMemberShare: number;
  esiMemberShare: number;
  netPayable: number;
}

@Component({
  selector: 'app-payroll-entry',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container-fluid py-4 min-vh-100 bg-light">
      
      <!-- 1. Header Section -->
      <div class="d-flex justify-content-between align-items-center mb-4 px-3">
        <div>
           <h2 class="fw-bold mb-0 text-dark">
             <i class="bi bi-grid-3x3-gap me-2 text-success"></i>Monthly Payroll Entry 
             <span *ngIf="period?.status === 'CLOSED'" class="badge bg-danger ms-2">LOCKED</span>
           </h2>
           <p class="text-muted mb-0 small" *ngIf="period">
              Period: {{ period.month }}/{{ period.year }} | Last Working Day: {{ period.lastWorkingDay }}
           </p>
        </div>

        <div class="d-flex gap-2">
           <!-- Import Controls -->
           <input type="file" #fileInput (change)="onFileSelected($event)" class="d-none" accept=".xlsx, .xls">
           
           <div class="btn-group" *ngIf="period?.status !== 'CLOSED'">
             <button class="btn btn-outline-primary" (click)="downloadTemplate()" [disabled]="isCalcLoading">
                <i class="bi bi-download"></i> Template
             </button>
             <button class="btn btn-outline-primary" (click)="fileInput.click()" [disabled]="isCalcLoading">
                <i class="bi bi-upload"></i> Import Excel
             </button>
           </div>

           <button class="btn btn-primary d-flex align-items-center" 
                   (click)="calculateAll()" [disabled]="period?.status === 'CLOSED' || isCalcLoading">
               <span *ngIf="isCalcLoading" class="spinner-border spinner-border-sm me-2"></span>
               <i class="bi bi-calculator me-2" *ngIf="!isCalcLoading"></i> Save & Calculate
           </button>
           
           <button *ngIf="period?.status !== 'CLOSED'" 
                   class="btn btn-success d-flex align-items-center" 
                   (click)="finalize()" [disabled]="isCalcLoading">
                <i class="bi bi-lock-fill me-2"></i> Finalize
           </button>

           <button *ngIf="period?.status === 'CLOSED'" 
                   class="btn btn-warning d-flex align-items-center" 
                   (click)="unlock()" [disabled]="isCalcLoading">
                <i class="bi bi-unlock-fill me-2"></i> Unlock
           </button>
        </div>
      </div>

      <!-- 2. Data Grid (Card) -->
      <div class="card border-0 shadow-sm mx-3 overflow-hidden rounded-3">
        <div class="table-responsive">
          <table class="table table-hover mb-0 align-middle">
            <thead class="bg-dark text-white">
              <tr>
                <th class="py-3 px-4">Employee</th>
                <th class="py-3 text-center" style="width: 140px;">Days Worked</th>
                <th class="py-3 text-center" style="width: 180px;">Wages Earned (₹)</th>
                <th class="py-3 text-center" style="width: 220px;">Advance (₹)</th>
                <th class="py-3 text-end px-4" style="width: 150px;">Net Pay</th>
              </tr>
            </thead>
            <tbody>
              <!-- Usage of TrackBy for performance if list is long -->
              <tr *ngFor="let row of entries" [class.table-secondary]="period?.status === 'CLOSED'">
                
                <!-- Employee Info -->
                <td class="px-4">
                    <div class="fw-bold text-dark">{{ row.employee.fullName }}</div>
                    <div class="small text-muted font-monospace">{{ row.employee.memberId }}</div>
                </td>
                
                <!-- Input: Days -->
                <td class="text-center">
                   <input type="number" class="form-control text-center fw-semibold border-0 bg-transparent" 
                          [(ngModel)]="row.daysWorked" min="0" max="31"
                          placeholder="0"
                          [disabled]="period?.status === 'CLOSED'">
                </td>



                <!-- Input: Wages -->
                <td class="text-center">
                   <input type="number" class="form-control text-center fw-semibold border-0 bg-transparent" 
                          [(ngModel)]="row.wagesEarned" min="0"
                          placeholder="0"
                          [disabled]="period?.status === 'CLOSED'">
                </td>

                <!-- Input: Advance -->
                <td class="text-center">
                   <div class="input-group input-group-sm">
                       <span class="input-group-text bg-transparent border-0"><i class="bi bi-dash-circle text-muted"></i></span>
                       <input type="number" class="form-control border-0 bg-transparent shadow-none" 
                              [(ngModel)]="row.advanceDeduction" min="0"
                              placeholder="0"
                              [disabled]="period?.status === 'CLOSED'">
                   </div>
                </td>

                <!-- Output: Net Pay -->
                <td class="text-end px-4">
                  <span class="badge bg-success-subtle text-success fs-6 rounded-pill px-3">
                      {{ row.netPayable | currency:'INR':'symbol':'1.0-0' }}
                  </span>
                </td>
              </tr>
              
              <!-- Empty State -->
              <tr *ngIf="entries.length === 0">
                 <td colspan="5" class="text-center py-5">
                    <div class="text-muted">Loading or No Employees Found...</div>
                 </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- Footer Note -->
      <div class="text-center mt-3 text-muted small">
          <i class="bi bi-info-circle me-1"></i> Values are auto-saved locally but must be 'Calculated' to update Net Pay.
      </div>
    </div>
  `
})

export class PayrollEntryComponent implements OnInit {
  periodId: string = 'latest';
  entries: PayrollEntry[] = [];
  period: PayrollPeriod | null = null;
  isCalcLoading = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private payrollService: PayrollService,
    private dialogService: DialogService
  ) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      if (params['periodId']) this.periodId = params['periodId'];
      this.loadData();
    });
  }

  loadData() {
    // Parallel Load (Optional) or Sequential
    this.payrollService.loadPeriod(this.periodId);
    this.payrollService.currentPeriod$.subscribe(p => this.period = p);

    this.http.get<PayrollEntry[]>(`${environment.apiUrl}/payroll/periods/${this.periodId}/entries`).subscribe({
      next: (data) => this.entries = data,
      error: (err) => console.error('Error loading payroll', err)
    });
  }

  downloadTemplate() {
    this.payrollService.downloadTemplate().subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) return;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'Payroll_Import_Template.xlsx';
        link.click();
      },
      error: (err) => this.dialogService.alert('Error', 'Failed to download template')
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && this.period) {
      this.isCalcLoading = true;
      this.payrollService.importEntries(file, this.period.id).subscribe({
        next: (res) => {
          this.dialogService.alert('Success', res.message);
          this.loadData(); // Refresh Grid
          this.isCalcLoading = false;
        },
        error: (err) => {
          this.dialogService.alert('Error', 'Import Failed: ' + (err.error?.message || err.message));
          this.isCalcLoading = false;
        }
      });
    }
    // Reset input
    event.target.value = '';
  }

  saveAndCalculate() {
    return this.http.put<PayrollEntry[]>(`${environment.apiUrl}/payroll/entries`, this.entries);
  }

  calculateAll() {
    this.isCalcLoading = true;
    this.saveAndCalculate().subscribe({
      next: (updatedEntries) => {
        this.entries = updatedEntries;
        this.sortEntries();
        this.isCalcLoading = false;
        this.dialogService.alert('Success', 'Saved & Calculated Successfully');
      },
      error: (err) => {
        console.error(err);
        this.isCalcLoading = false;
        this.dialogService.alert('Error', 'Failed to save: ' + (err.error?.message || err.message));
      }
    });
  }

  finalize() {
    this.dialogService.confirm('Finalize Period', 'Are you sure you want to Finalize? This will SAVE current data and LOCK the period.')
      .subscribe(confirmed => {
        if (!confirmed) return;

        if (this.period) {
          this.isCalcLoading = true; // Show loading state

          // Step 1: Save & Calculate First
          this.saveAndCalculate().subscribe({
            next: (updatedEntries) => {
              this.entries = updatedEntries;
              this.sortEntries();

              // Step 2: Close Period
              this.payrollService.closePeriod(this.period!.id).subscribe({
                next: () => {
                  this.isCalcLoading = false;
                  this.dialogService.alert('Success', 'Period Finalized Successfully!');
                  this.loadData();
                },
                error: (err) => {
                  this.isCalcLoading = false;
                  this.dialogService.alert('Error', 'Saved, but failed to finalize: ' + err.message);
                }
              });
            },
            error: (err) => {
              this.isCalcLoading = false;
              this.dialogService.alert('Error', 'Cannot Finalize: Failed to save data. ' + err.message);
            }
          });
        }
      });
  }

  unlock() {
    this.dialogService.confirm('Unlock Period', 'Are you sure you want to UNLOCK this period for editing?')
      .subscribe(confirmed => {
        if (!confirmed) return;

        if (this.period) {
          this.payrollService.reopenPeriod(this.period.id).subscribe({
            next: () => {
              this.dialogService.alert('Success', 'Period Unlocked!');
              this.loadData();
            },
            error: (err) => this.dialogService.alert('Error', 'Failed to unlock: ' + err.message)
          });
        }
      });
  }

  private sortEntries() {
    this.entries.sort((e1, e2) => {
      const n1 = parseInt(e1.employee.memberId);
      const n2 = parseInt(e2.employee.memberId);
      if (!isNaN(n1) && !isNaN(n2)) return n1 - n2;
      return e1.employee.memberId.localeCompare(e2.employee.memberId);
    });
  }
}
