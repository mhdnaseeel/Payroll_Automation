import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { SalaryService, DailyBagsWork, DailyClauseWork, DailyHeadcount, SalaryCalculationResult } from './salary.service';

@Component({
    selector: 'app-salary',
    standalone: true,
    imports: [CommonModule, FormsModule],
    providers: [DatePipe],
    template: `
    <div class="container-fluid py-4 fade-in">
        
        <!-- Controls Header -->
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-center bg-white p-3 rounded-4 shadow-sm mb-4">
            <div class="d-flex align-items-center mb-3 mb-md-0">
               <div class="btn-group" role="group">
                  <button type="button" class="btn fw-semibold" 
                          [ngClass]="viewMode === 'entry' ? 'btn-primary shadow-sm' : 'btn-outline-secondary border-0'" 
                          (click)="viewMode = 'entry'">
                    <i class="bi bi-keyboard me-2"></i>Data Entry
                  </button>
                  <button type="button" class="btn fw-semibold" 
                          [ngClass]="viewMode === 'viewer' ? 'btn-primary shadow-sm' : 'btn-outline-secondary border-0'" 
                          (click)="switchToViewer()">
                    <i class="bi bi-clock-history me-2"></i>History Viewer
                  </button>
               </div>
            </div>
            
            <div *ngIf="viewMode === 'entry'" class="d-flex align-items-center gap-2">
                <span class="text-secondary fw-semibold">Work Date:</span>
                <input type="date" class="form-control form-control-sm border-2 rounded-3 w-auto" 
                       [(ngModel)]="selectedDate" (change)="loadDailyData()">
            </div>
        </div>

        <!-- Notification Alert -->
        <div *ngIf="message" class="alert alert-dismissible fade show shadow-sm" [ngClass]="isError ? 'alert-danger' : 'alert-success'" role="alert">
            <i class="bi me-2" [ngClass]="isError ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'"></i>
            <strong>{{ message }}</strong>
            <button type="button" class="btn-close" (click)="message = ''"></button>
        </div>

        <!-- ======================= DATA ENTRY MODE ======================= -->
        <ng-container *ngIf="viewMode === 'entry'">
          <div class="row g-4">
              
              <!-- Table 1 -->
              <div class="col-lg-6">
                  <div class="card border-0 shadow-sm rounded-4 h-100 hover-card">
                      <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                          <h5 class="fw-bold mb-0 text-primary">
                             <i class="bi bi-box-seam me-2"></i> Table 1: Stacking Quantities
                          </h5>
                          <hr class="mt-3 mb-0 text-primary opacity-25">
                      </div>
                      <div class="card-body p-4">
                          <form>
                              <div class="mb-4">
                                  <label class="form-label text-secondary small fw-bold text-uppercase">Work Slip No.</label>
                                  <input type="text" class="form-control form-control-lg bg-light border-0" [(ngModel)]="bagsWork.workSlipNo" name="slipNo1" placeholder="Enter Slip No.">
                              </div>
                              <div class="row g-3">
                                  <div class="col-sm-6">
                                      <label class="form-label text-secondary small fw-bold text-uppercase">
                                          Up to 10 High <span class="badge bg-primary-subtle text-primary border border-primary-subtle ms-1">₹7.98</span>
                                      </label>
                                      <input type="number" class="form-control form-control-lg bg-light border-0" [(ngModel)]="bagsWork.bagsUpto10" name="b10" placeholder="0">
                                  </div>
                                  <div class="col-sm-6">
                                      <label class="form-label text-secondary small fw-bold text-uppercase">
                                          11-16 High <span class="badge bg-primary-subtle text-primary border border-primary-subtle ms-1">₹9.37</span>
                                      </label>
                                      <input type="number" class="form-control form-control-lg bg-light border-0" [(ngModel)]="bagsWork.bags11to16" name="b16" placeholder="0">
                                  </div>
                                  <div class="col-sm-6">
                                      <label class="form-label text-secondary small fw-bold text-uppercase">
                                          17-20 High <span class="badge bg-primary-subtle text-primary border border-primary-subtle ms-1">₹10.83</span>
                                      </label>
                                      <input type="number" class="form-control form-control-lg bg-light border-0" [(ngModel)]="bagsWork.bags17to20" name="b20" placeholder="0">
                                  </div>
                                  <div class="col-sm-6">
                                      <label class="form-label text-secondary small fw-bold text-uppercase">
                                          Above 20 <span class="badge bg-primary-subtle text-primary border border-primary-subtle ms-1">₹14.16</span>
                                      </label>
                                      <input type="number" class="form-control form-control-lg bg-light border-0" [(ngModel)]="bagsWork.bagsAbove20" name="bAbove" placeholder="0">
                                  </div>
                              </div>
                              <button class="btn btn-primary w-100 mt-4 rounded-3 fw-bold py-2 mt-auto" [disabled]="isSaving" (click)="saveBags()">
                                 <i class="bi bi-save me-2"></i> Save Stacking Data
                              </button>
                          </form>
                      </div>
                  </div>
              </div>

              <!-- Right Column (Table 4 + Summary) -->
              <div class="col-lg-6 d-flex flex-column gap-4">
                  
                  <!-- Table 4 -->
                  <div class="card border-0 shadow-sm rounded-4 hover-card">
                      <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                          <h5 class="fw-bold mb-0" style="color: #6f42c1;">
                             <i class="bi bi-tag me-2"></i> Table 4: Issue / Clause XIX
                          </h5>
                          <hr class="mt-3 mb-0 opacity-25" style="color: #6f42c1;">
                      </div>
                      <div class="card-body p-4">
                          <form>
                              <div class="mb-4">
                                  <label class="form-label text-secondary small fw-bold text-uppercase">Work Slip No.</label>
                                  <input type="text" class="form-control form-control-lg bg-light border-0" [(ngModel)]="clauseWork.workSlipNo" name="slipNo4" placeholder="Enter Slip No.">
                              </div>
                              <div class="mb-4">
                                  <label class="form-label text-secondary small fw-bold text-uppercase">
                                      Clause 15 Bags <span class="badge ms-1" style="background-color: #f3e8fa; color: #6f42c1; border: 1px solid #e2c0fa;">₹9.65</span>
                                  </label>
                                  <input type="number" class="form-control form-control-lg bg-light border-0" [(ngModel)]="clauseWork.bagsClause15" name="clause15" placeholder="0">
                              </div>
                              <button class="btn text-white w-100 rounded-3 fw-bold py-2" style="background-color: #6f42c1;" [disabled]="isSaving" (click)="saveClause()">
                                 <i class="bi bi-save me-2"></i> Save Issue Data
                              </button>
                          </form>
                      </div>
                  </div>

                  <!-- Table 2: Active Headcount Override -->
                  <div class="card border-0 shadow-sm rounded-4 hover-card" style="border-left: 5px solid #0dcaf0 !important;">
                      <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                          <h5 class="fw-bold mb-0 text-info">
                             <i class="bi bi-people me-2"></i> Active Headcount
                          </h5>
                          <hr class="mt-3 mb-0 text-info opacity-25">
                      </div>
                      <div class="card-body p-4">
                          <div class="d-flex align-items-center gap-3">
                              <div class="flex-grow-1">
                                  <label class="form-label text-secondary small fw-bold text-uppercase">Manual Count</label>
                                  <input type="number" class="form-control form-control-lg bg-light border-0" 
                                         [(ngModel)]="headcountEntry.headcount" name="hc" placeholder="Enter headcount">
                              </div>
                              <button class="btn btn-info text-white rounded-3 fw-bold px-4 py-2 mt-4" 
                                      [disabled]="isSaving" (click)="saveHeadcount()">
                                 <i class="bi bi-save me-1"></i> Save
                              </button>
                          </div>
                          <div class="mt-2 small text-secondary">
                              <i class="bi bi-info-circle me-1"></i> This overrides the automatic attendance logs for this date.
                          </div>
                      </div>
                  </div>

                  <!-- Summary Widget -->
                  <div class="card text-white border-0 shadow-lg rounded-4 overflow-hidden" *ngIf="calculation" style="background: linear-gradient(135deg, #1e293b, #0f172a);">
                      <div class="card-body p-5 position-relative">
                          <i class="bi bi-currency-rupee position-absolute text-white opacity-10" style="font-size: 8rem; top: -1rem; right: -1rem;"></i>
                          
                          <h6 class="text-uppercase tracking-wider text-secondary mb-1">Live Allocation Pool</h6>
                          <p class="text-light opacity-75 small mb-4">{{ calculation.date | date:'EEEE, MMM d, yyyy' }}</p>
                          
                          <div class="mb-4">
                              <span class="d-block text-secondary small">Total Pool Value</span>
                              <span class="fs-1 fw-bold text-success">₹{{ calculation.totalAmount | number:'1.2-2' }}</span>
                          </div>
                          
                          <div class="row border-top border-secondary pt-4 mt-2">
                              <div class="col-6 border-end border-secondary border-opacity-50">
                                  <span class="d-block text-secondary small mb-1">Active Laborers</span>
                                  <span class="fs-4 fw-bold text-info">{{ calculation.totalAttendance }} <span class="fs-6 fw-normal text-light opacity-75">present</span></span>
                              </div>
                              <div class="col-6 ps-4">
                                  <span class="d-block text-secondary small mb-1">Per Person Share</span>
                                  <span class="fs-4 fw-bold text-white">₹{{ calculation.perPersonSalary | number:'1.2-2' }}</span>
                              </div>
                          </div>

                          <div *ngIf="calculation.totalAttendance === 0 && calculation.totalAmount > 0" class="alert alert-danger mt-4 border-0 d-flex align-items-center rounded-3 bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 p-3">
                              <i class="bi bi-exclamation-triangle-fill fs-4 me-3"></i>
                              <div>
                                  <strong class="d-block">No Attendance Logged</strong>
                                  <span class="small">Mark attendance in the Dashboard first to calculate correct shares.</span>
                              </div>
                          </div>
                      </div>
                  </div>
              </div>
          </div>
        </ng-container>

        <!-- ======================= VIEWER MODE ======================= -->
        <ng-container *ngIf="viewMode === 'viewer'">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-header bg-white border-bottom-0 p-4 pb-0 d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3">
                    <div>
                        <h4 class="fw-bold mb-1 text-primary"><i class="bi bi-calendar-range me-2"></i>Historical Allocations</h4>
                        <p class="text-secondary small mb-0">Review total pool values and per-person allocations over time.</p>
                    </div>
                    <div class="d-flex align-items-center gap-2 bg-light p-2 rounded-3">
                       <input type="date" class="form-control form-control-sm border-0 bg-white shadow-sm" [(ngModel)]="rangeStart">
                       <span class="text-secondary fw-bold px-1">to</span>
                       <input type="date" class="form-control form-control-sm border-0 bg-white shadow-sm" [(ngModel)]="rangeEnd">
                       <button class="btn btn-sm btn-primary px-3 shadow-sm rounded-3" (click)="loadRangeData()" [disabled]="isLoadingRange">
                           <span *ngIf="isLoadingRange" class="spinner-border spinner-border-sm me-1"></span>
                           <span *ngIf="!isLoadingRange"><i class="bi bi-search"></i></span>
                       </button>
                    </div>
                </div>
                
                <div class="card-body p-4">
                    <div class="table-responsive rounded-3 border">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="py-3 px-4 text-secondary fw-semibold text-uppercase small">Work Date</th>
                                    <th class="py-3 px-4 text-secondary fw-semibold text-uppercase small">Total Amount (₹)</th>
                                    <th class="py-3 px-4 text-secondary fw-semibold text-uppercase small text-center">Active Headcount</th>
                                    <th class="py-3 px-4 bg-primary text-white fw-semibold text-uppercase small">Per Person Share (₹)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr *ngIf="rangeCalculations.length === 0 && !isLoadingRange">
                                    <td colspan="4" class="text-center py-5 text-secondary">
                                        <i class="bi bi-folder-x fs-1 opacity-50 d-block mb-2"></i>
                                        No calculation data recorded for this date range.
                                    </td>
                                </tr>
                                <tr *ngFor="let item of rangeCalculations">
                                    <td class="px-4 py-3 fw-medium">{{ item.date | date:'longDate' }}</td>
                                    <td class="px-4 py-3 fw-bold text-success">₹ {{ item.totalAmount | number:'1.2-2' }}</td>
                                    <td class="px-4 py-3 text-center">
                                        <span class="badge bg-secondary rounded-pill px-3">{{ item.totalAttendance }}</span>
                                    </td>
                                    <td class="px-4 py-3 fw-bold text-primary bg-primary bg-opacity-10 border-start border-primary border-opacity-25">₹ {{ item.perPersonSalary | number:'1.2-2' }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </ng-container>

    </div>
    `,
    styles: [`
    input[type="number"]::-webkit-inner-spin-button, 
    input[type="number"]::-webkit-outer-spin-button { 
      -webkit-appearance: none; 
      margin: 0; 
    }
    
    .hover-card {
        transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
    }
    .hover-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.1) !important;
    }

    .fade-in {
        animation: fadeIn 0.4s ease-out forwards;
    }

    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(15px); }
        to { opacity: 1; transform: translateY(0); }
    }
    `]
})
export class SalaryComponent implements OnInit {

    viewMode: 'entry' | 'viewer' = 'entry';
    selectedDate: string;
    
    // Viewer params
    rangeStart: string;
    rangeEnd: string;
    rangeCalculations: SalaryCalculationResult[] = [];
    isLoadingRange = false;

    isSaving = false;
    message = '';
    isError = false;

    bagsWork: DailyBagsWork = {
        date: '',
        workSlipNo: '',
        bagsUpto10: 0,
        bags11to16: 0,
        bags17to20: 0,
        bagsAbove20: 0
    };

    clauseWork: DailyClauseWork = {
        date: '',
        workSlipNo: '',
        bagsClause15: 0
    };

    headcountEntry: DailyHeadcount = {
        date: '',
        headcount: 0
    };

    calculation: SalaryCalculationResult | null = null;

    constructor(
        private titleService: Title,
        private salaryService: SalaryService,
        private datePipe: DatePipe
    ) {
        const today = new Date();
        this.selectedDate = today.toISOString().split('T')[0];
        
        // Default range to first day of current month to today
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        this.rangeStart = firstDay.toISOString().split('T')[0];
        this.rangeEnd = this.selectedDate;

        this.updateLocalDates();
    }

    ngOnInit(): void {
        this.titleService.setTitle('HR Automation - Salary Calculator');
        this.loadDailyData();
    }

    updateLocalDates() {
        this.bagsWork.date = this.selectedDate;
        this.clauseWork.date = this.selectedDate;
        this.headcountEntry.date = this.selectedDate;
    }

    showMessage(msg: string, error: boolean = false) {
        this.message = msg;
        this.isError = error;
        setTimeout(() => this.message = '', 4000);
    }

    loadDailyData() {
        this.updateLocalDates();
        this.calculation = null;

        this.salaryService.getDailyCalculation(this.selectedDate).subscribe({
            next: (res) => {
                this.calculation = res;
            },
            error: (err) => {
                console.error('Failed to load calculations', err);
            }
        });
    }

    saveBags() {
        this.isSaving = true;
        this.updateLocalDates();
        this.salaryService.saveBagsWork(this.bagsWork).subscribe({
            next: () => {
                this.showMessage('Stacking Data saved successfully!');
                this.loadDailyData();
                this.isSaving = false;
            },
            error: (err) => {
                console.error(err);
                this.showMessage(err.error?.message || 'Failed to save Table 1 data.', true);
                this.isSaving = false;
            }
        });
    }

    saveClause() {
        this.isSaving = true;
        this.updateLocalDates();
        this.salaryService.saveClauseWork(this.clauseWork).subscribe({
            next: () => {
                this.showMessage('Issue Data saved successfully!');
                this.loadDailyData();
                this.isSaving = false;
            },
            error: (err) => {
                console.error(err);
                this.showMessage(err.error?.message || 'Failed to save Table 4 data.', true);
                this.isSaving = false;
            }
        });
    }

    saveHeadcount() {
        if (!this.headcountEntry.headcount || this.headcountEntry.headcount < 0) {
            this.showMessage('Please enter a valid headcount.', true);
            return;
        }
        this.isSaving = true;
        this.updateLocalDates();
        this.salaryService.saveHeadcount(this.headcountEntry).subscribe({
            next: () => {
                this.showMessage('Headcount override saved successfully!');
                this.loadDailyData();
                this.isSaving = false;
            },
            error: (err) => {
                console.error(err);
                this.showMessage(err.error?.message || 'Failed to save headcount override.', true);
                this.isSaving = false;
            }
        });
    }

    switchToViewer() {
        this.viewMode = 'viewer';
        this.loadRangeData();
    }

    loadRangeData() {
        if (!this.rangeStart || !this.rangeEnd) return;
        this.isLoadingRange = true;
        this.salaryService.getCalculationsRange(this.rangeStart, this.rangeEnd).subscribe({
            next: (data) => {
                this.rangeCalculations = data;
                this.isLoadingRange = false;
            },
            error: (err) => {
                console.error('Failed to load range calculation', err);
                this.showMessage('Failed to load historical data', true);
                this.isLoadingRange = false;
            }
        });
    }
}
