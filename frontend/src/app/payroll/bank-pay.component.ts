import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../environments/environment';

export interface BankPayRecord {
  employeeId?: string;
  employeeName: string;
  accountNumber?: string;
  ifscCode?: string;
  amount: number;
  status: 'READY' | 'NOT_FOUND' | 'INVALID';
  reason?: string;
  bankCategory?: 'SAME_BANK' | 'OTHER_BANK';
}

export interface BankPayImportResult {
  records: BankPayRecord[];
  totalRecords: number;
  totalSuccess: number;
  totalFailed: number;
  sameBankCount: number;
  sameBankAmount: number;
  otherBankCount: number;
  otherBankAmount: number;
  totalAmount: number;
}

@Component({
  selector: 'app-bank-pay',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container-fluid py-4 px-md-5 fade-in">
      <!-- Breadcrumb & Top Bar -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <nav aria-label="breadcrumb">
            <ol class="breadcrumb mb-1">
              <li class="breadcrumb-item"><a routerLink="/user/home" class="text-decoration-none text-muted"><i class="bi bi-house-door"></i> Dashboard</a></li>
              <li class="breadcrumb-item active fw-semibold text-primary" aria-current="page">Bank Pay</li>
            </ol>
          </nav>
          <h2 class="fw-bold mb-0 text-dark d-flex align-items-center gap-2">
            <span class="p-2 rounded-3 bg-primary-subtle text-primary d-inline-flex align-items-center justify-content-center" style="width: 42px; height: 42px;">
              <i class="bi bi-bank"></i>
            </span>
            Bulk Payment Module (SBI)
            <span class="badge bg-gradient-primary rounded-pill text-white fs-6 px-3 ms-2 shadow-sm">Standalone</span>
          </h2>
        </div>
        <div class="mt-3 mt-md-0">
          <button class="btn btn-outline-secondary rounded-pill me-2" (click)="downloadSampleTemplate()">
            <i class="bi bi-file-earmark-excel me-1"></i> Sample Template
          </button>
          <a routerLink="/user/home" class="btn btn-outline-primary rounded-pill">
            <i class="bi bi-arrow-left me-1"></i> Back to Dashboard
          </a>
        </div>
      </div>

      <!-- Main Layout -->
      <div class="row g-4">
        <!-- Left Panel: Import & Configuration -->
        <div class="col-lg-4">
          <div class="card border-0 shadow-sm rounded-4 h-100 bg-white overflow-hidden">
            <div class="card-header bg-gradient-dark text-white p-3 border-0">
              <h5 class="mb-0 fw-bold d-flex align-items-center gap-2">
                <i class="bi bi-cloud-upload"></i> Step 1: Import Excel Payout
              </h5>
            </div>
            <div class="card-body p-4">
              <!-- Upload Box -->
              <div class="upload-dropzone p-4 text-center rounded-3 border-2 border-dashed mb-4"
                   [class.border-primary]="isHovered"
                   [class.bg-primary-subtle]="isHovered"
                   (dragover)="onDragOver($event)"
                   (dragleave)="onDragLeave($event)"
                   (drop)="onDrop($event)">
                <div class="icon-circle mb-3 mx-auto bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center" style="width: 60px; height: 60px;">
                  <i class="bi bi-file-earmark-spreadsheet fs-2"></i>
                </div>
                <h6 class="fw-bold mb-1">Select or Drop Excel File</h6>
                <p class="text-muted small mb-3">Columns <code>Employee Name</code> and <code>Amount</code> required.</p>
                
                <input type="file" #fileInput class="d-none" accept=".xlsx, .xls" (change)="onFileSelected($event)">
                <button class="btn btn-primary rounded-pill px-4" (click)="fileInput.click()">
                  <i class="bi bi-folder2-open me-1"></i> Browse File
                </button>
                <div *ngIf="selectedFile" class="mt-3 p-2 bg-light rounded text-success fw-medium small text-truncate">
                  <i class="bi bi-check-circle-fill me-1"></i> {{ selectedFile.name }}
                </div>
              </div>

              <!-- Action Button -->
              <button class="btn btn-success w-100 rounded-pill py-2.5 fw-bold shadow-sm mb-4"
                      [disabled]="!selectedFile || isProcessing"
                      (click)="uploadAndMatch()">
                <span *ngIf="isProcessing" class="spinner-border spinner-border-sm me-2" role="status"></span>
                <i *ngIf="!isProcessing" class="bi bi-search me-1"></i>
                {{ isProcessing ? 'Processing & Classifying...' : 'Import & Classify Payments' }}
              </button>

              <hr class="my-4">

              <!-- Bank File Configuration -->
              <h6 class="fw-bold mb-3 text-dark d-flex align-items-center gap-2">
                <i class="bi bi-sliders text-primary"></i> SBI Corporate Account Settings
              </h6>
              
              <div class="mb-3">
                <label class="form-label small fw-bold text-muted">Payment Execution Date</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-end-0"><i class="bi bi-calendar3"></i></span>
                  <input type="date" class="form-control border-start-0" [(ngModel)]="paymentDate">
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold text-muted">Company Account Number</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-end-0"><i class="bi bi-credit-card"></i></span>
                  <input type="text" class="form-control border-start-0" [(ngModel)]="companyAccount" placeholder="e.g. 44145351821">
                </div>
              </div>

              <div class="row g-2 mb-3">
                <div class="col-6">
                  <label class="form-label small fw-bold text-muted">Branch Code</label>
                  <input type="text" class="form-control" [(ngModel)]="branchCode" placeholder="17242">
                </div>
                <div class="col-6">
                  <label class="form-label small fw-bold text-muted">SBI Corporate Format</label>
                  <input type="text" class="form-control bg-light" value="ST / NEFT" readonly>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label small fw-bold text-muted">Company Name (Payer)</label>
                <div class="input-group">
                  <span class="input-group-text bg-light border-end-0"><i class="bi bi-building"></i></span>
                  <input type="text" class="form-control border-start-0" [(ngModel)]="companyName" placeholder="NASAR PK">
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- Right Panel: Processing Summary & Record Preview -->
        <div class="col-lg-8">
          <!-- Processing Summary Header -->
          <div class="d-flex align-items-center justify-content-between mb-2">
            <h5 class="fw-bold mb-0 text-dark"><i class="bi bi-pie-chart-fill text-primary me-1"></i> Processing Summary</h5>
            <button *ngIf="importResult && importResult.totalFailed > 0"
                    class="btn btn-outline-danger btn-sm rounded-pill shadow-sm"
                    (click)="downloadExceptionReport()">
              <i class="bi bi-file-earmark-excel me-1"></i> Exception Report (.xlsx)
            </button>
          </div>

          <!-- Processing Summary Metric Cards Grid -->
          <div class="row g-3 mb-4">
            <!-- 1. Total Records -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-white p-3 h-100 border-start border-4 border-secondary">
                <div class="text-muted small fw-bold text-uppercase">Total Uploaded</div>
                <div class="fs-3 fw-extrabold text-dark mt-1">{{ importResult?.totalRecords || 0 }}</div>
                <div class="text-muted small mt-1">Excel Rows</div>
              </div>
            </div>

            <!-- 2. Successfully Processed -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-white p-3 h-100 border-start border-4 border-success">
                <div class="text-success small fw-bold text-uppercase">Processed (Success)</div>
                <div class="fs-3 fw-extrabold text-success mt-1">{{ importResult?.totalSuccess || 0 }}</div>
                <div class="text-muted small mt-1">Matched & Validated</div>
              </div>
            </div>

            <!-- 3. Failed / Exceptions -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-white p-3 h-100 border-start border-4 border-danger">
                <div class="text-danger small fw-bold text-uppercase">Failed (Exceptions)</div>
                <div class="fs-3 fw-extrabold text-danger mt-1">{{ importResult?.totalFailed || 0 }}</div>
                <div class="text-muted small mt-1">Excluded from payouts</div>
              </div>
            </div>

            <!-- 4. Same Bank (SBI to SBI) -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-white p-3 h-100 border-start border-4 border-primary">
                <div class="text-primary small fw-bold text-uppercase"><i class="bi bi-bank me-1"></i> Same Bank (SBI)</div>
                <div class="fs-4 fw-extrabold text-primary mt-1">₹ {{ (importResult?.sameBankAmount || 0) | number:'1.2-2' }}</div>
                <div class="text-muted small mt-1">{{ importResult?.sameBankCount || 0 }} SBI Payouts</div>
              </div>
            </div>

            <!-- 5. Other Bank (NEFT) -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-white p-3 h-100 border-start border-4 border-purple">
                <div class="text-purple small fw-bold text-uppercase"><i class="bi bi-arrow-right-circle me-1"></i> Other Bank (NEFT)</div>
                <div class="fs-4 fw-extrabold text-purple mt-1">₹ {{ (importResult?.otherBankAmount || 0) | number:'1.2-2' }}</div>
                <div class="text-muted small mt-1">{{ importResult?.otherBankCount || 0 }} Interbank Payouts</div>
              </div>
            </div>

            <!-- 6. Total Payment Amount -->
            <div class="col-6 col-md-4">
              <div class="card border-0 shadow-sm rounded-4 bg-gradient-success text-white p-3 h-100 shadow">
                <div class="opacity-75 small fw-bold text-uppercase">Total Batch Amount</div>
                <div class="fs-4 fw-extrabold mt-1">₹ {{ (importResult?.totalAmount || 0) | number:'1.2-2' }}</div>
                <div class="opacity-75 small mt-1">Combined Net Payout</div>
              </div>
            </div>
          </div>

          <!-- Preview Table Card -->
          <div class="card border-0 shadow-sm rounded-4 bg-white overflow-hidden mb-4">
            <div class="card-header bg-white p-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
              <div class="d-flex align-items-center gap-2">
                <h5 class="mb-0 fw-bold text-dark"><i class="bi bi-table text-primary"></i> Step 2: Classified Record Preview</h5>
                <span class="badge bg-secondary-subtle text-secondary rounded-pill" *ngIf="importResult">
                  {{ filteredRecords.length }} records
                </span>
              </div>

              <!-- Filter Tabs -->
              <div class="btn-group btn-group-sm rounded-pill p-1 bg-light">
                <button class="btn rounded-pill border-0 px-3" [class.btn-white]="activeFilter === 'ALL'" [class.shadow-sm]="activeFilter === 'ALL'" (click)="activeFilter = 'ALL'">
                  All ({{ importResult?.records?.length || 0 }})
                </button>
                <button class="btn rounded-pill border-0 px-3 text-primary" [class.btn-white]="activeFilter === 'SAME_BANK'" [class.shadow-sm]="activeFilter === 'SAME_BANK'" (click)="activeFilter = 'SAME_BANK'">
                  SBI ({{ importResult?.sameBankCount || 0 }})
                </button>
                <button class="btn rounded-pill border-0 px-3 text-purple" [class.btn-white]="activeFilter === 'OTHER_BANK'" [class.shadow-sm]="activeFilter === 'OTHER_BANK'" (click)="activeFilter = 'OTHER_BANK'">
                  Other Bank ({{ importResult?.otherBankCount || 0 }})
                </button>
                <button class="btn rounded-pill border-0 px-3 text-danger" [class.btn-white]="activeFilter === 'FAILED'" [class.shadow-sm]="activeFilter === 'FAILED'" (click)="activeFilter = 'FAILED'">
                  Failed ({{ importResult?.totalFailed || 0 }})
                </button>
              </div>
            </div>

            <div class="card-body p-0">
              <div class="table-responsive" style="max-height: 380px;">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light sticky-top">
                    <tr>
                      <th class="ps-3">Employee ID</th>
                      <th>Employee Name</th>
                      <th>Bank Type</th>
                      <th>Account Number</th>
                      <th>IFSC Code</th>
                      <th class="text-end">Amount (₹)</th>
                      <th class="text-center">Status & Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngIf="!importResult || importResult.records.length === 0">
                      <td colspan="7" class="text-center py-5 text-muted">
                        <i class="bi bi-file-earmark-x display-4 d-block opacity-50 mb-2"></i>
                        No file imported yet. Upload an Excel file on the left panel to preview records.
                      </td>
                    </tr>

                    <tr *ngFor="let rec of filteredRecords" [class.table-danger-subtle]="rec.status !== 'READY'">
                      <td class="ps-3 fw-bold text-secondary">
                        {{ rec.employeeId || '-' }}
                      </td>
                      <td>
                        <span class="fw-semibold text-dark">{{ rec.employeeName }}</span>
                      </td>
                      <td>
                        <span *ngIf="rec.status === 'READY' && rec.bankCategory === 'SAME_BANK'" class="badge bg-primary-subtle text-primary border border-primary rounded-pill px-2.5 py-1">
                          <i class="bi bi-bank me-1"></i> SBI Same Bank
                        </span>
                        <span *ngIf="rec.status === 'READY' && rec.bankCategory === 'OTHER_BANK'" class="badge bg-purple-subtle text-purple border border-purple rounded-pill px-2.5 py-1">
                          <i class="bi bi-arrow-right-circle me-1"></i> Other Bank (NEFT)
                        </span>
                        <span *ngIf="rec.status !== 'READY'" class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-1">
                          Unclassified
                        </span>
                      </td>
                      <td>
                        <code class="text-dark bg-light px-2 py-1 rounded" *ngIf="rec.accountNumber">{{ rec.accountNumber }}</code>
                        <span class="text-muted italic small" *ngIf="!rec.accountNumber">Not available</span>
                      </td>
                      <td>
                        <span class="badge bg-light text-dark border" *ngIf="rec.ifscCode">{{ rec.ifscCode }}</span>
                        <span class="text-muted italic small" *ngIf="!rec.ifscCode">-</span>
                      </td>
                      <td class="text-end fw-bold text-dark">
                        ₹ {{ rec.amount | number:'1.2-2' }}
                      </td>
                      <td class="text-center">
                        <span *ngIf="rec.status === 'READY'" class="badge bg-success-subtle text-success border border-success rounded-pill px-3 py-1">
                          <i class="bi bi-check-circle-fill me-1"></i> Ready
                        </span>
                        <span *ngIf="rec.status === 'NOT_FOUND'" class="badge bg-danger-subtle text-danger border border-danger rounded-pill px-3 py-1" [title]="rec.reason">
                          <i class="bi bi-person-x-fill me-1"></i> Not Found
                        </span>
                        <span *ngIf="rec.status === 'INVALID'" class="badge bg-warning-subtle text-warning-emphasis border border-warning rounded-pill px-3 py-1" [title]="rec.reason">
                          <i class="bi bi-exclamation-triangle-fill me-1"></i> Invalid
                        </span>
                        <div *ngIf="rec.reason && rec.status !== 'READY'" class="text-danger small mt-1" style="font-size: 0.75rem;">
                          {{ rec.reason }}
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Footer Action Bar (Download Buttons) -->
            <div class="card-footer bg-light p-3 d-flex flex-wrap justify-content-between align-items-center border-top gap-2">
              <div class="small text-muted">
                <i class="bi bi-info-circle me-1"></i> Select SBI transfer file format to download.
              </div>

              <div class="d-flex flex-wrap gap-2">
                <!-- 1. SBI Same Bank Button -->
                <button class="btn btn-primary rounded-pill px-3 py-2 fw-bold shadow-sm"
                        [disabled]="!importResult || importResult.sameBankCount === 0 || isGenerating"
                        (click)="generateBankFile('SAME_BANK')">
                  <i class="bi bi-bank me-1"></i> SBI Same Bank (.txt)
                </button>

                <!-- 2. Other Bank Button -->
                <button class="btn btn-purple rounded-pill px-3 py-2 fw-bold text-white shadow-sm"
                        [disabled]="!importResult || importResult.otherBankCount === 0 || isGenerating"
                        (click)="generateBankFile('OTHER_BANK')">
                  <i class="bi bi-arrow-right-circle me-1"></i> Other Bank NEFT (.txt)
                </button>

                <!-- 3. Exception Report Button -->
                <button *ngIf="importResult && importResult.totalFailed > 0"
                        class="btn btn-outline-danger rounded-pill px-3 py-2 fw-semibold shadow-sm"
                        (click)="downloadExceptionReport()">
                  <i class="bi bi-file-earmark-excel me-1"></i> Exception Report (.xlsx)
                </button>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .upload-dropzone {
      background-color: #f8fafc;
      border-color: #cbd5e1;
      transition: all 0.2s ease-in-out;
      cursor: pointer;
    }
    .upload-dropzone:hover {
      background-color: #f1f5f9;
      border-color: #94a3b8;
    }
    .bg-gradient-dark {
      background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
    }
    .bg-gradient-primary {
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    }
    .bg-gradient-success {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    }
    .text-purple {
      color: #7c3aed !important;
    }
    .bg-purple-subtle {
      background-color: #f3e8ff !important;
    }
    .border-purple {
      border-color: #c4b5fd !important;
    }
    .btn-purple {
      background-color: #7c3aed;
      border-color: #7c3aed;
    }
    .btn-purple:hover {
      background-color: #6d28d9;
      border-color: #6d28d9;
    }
    .table-hover tbody tr:hover {
      background-color: rgba(37, 99, 235, 0.03);
    }
    .table-danger-subtle {
      background-color: #fef2f2;
    }
    .btn-white {
      background-color: #ffffff;
      color: #0f172a;
    }
    .fw-extrabold {
      font-weight: 800;
    }
  `]
})
export class BankPayComponent {
  selectedFile: File | null = null;
  isHovered = false;
  isProcessing = false;
  isGenerating = false;

  paymentDate: string = new Date().toISOString().split('T')[0];
  companyAccount: string = '44145351821';
  branchCode: string = '17242';
  companyName: string = 'NASAR PK';

  importResult: BankPayImportResult | null = null;
  activeFilter: 'ALL' | 'SAME_BANK' | 'OTHER_BANK' | 'FAILED' = 'ALL';

  constructor(private http: HttpClient) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isHovered = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isHovered = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isHovered = false;
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.selectedFile = event.dataTransfer.files[0];
    }
  }

  onFileSelected(event: any) {
    if (event.target.files && event.target.files.length > 0) {
      this.selectedFile = event.target.files[0];
    }
  }

  uploadAndMatch() {
    if (!this.selectedFile) return;

    this.isProcessing = true;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post<BankPayImportResult>(`${environment.apiUrl}/bank-pay/import`, formData).subscribe({
      next: (res) => {
        this.importResult = res;
        this.isProcessing = false;
      },
      error: (err) => {
        console.error('Import error', err);
        alert('Failed to process Excel file. Please ensure it is a valid .xlsx or .xls file.');
        this.isProcessing = false;
      }
    });
  }

  get filteredRecords(): BankPayRecord[] {
    if (!this.importResult || !this.importResult.records) return [];
    if (this.activeFilter === 'SAME_BANK') {
      return this.importResult.records.filter(r => r.status === 'READY' && r.bankCategory === 'SAME_BANK');
    }
    if (this.activeFilter === 'OTHER_BANK') {
      return this.importResult.records.filter(r => r.status === 'READY' && r.bankCategory === 'OTHER_BANK');
    }
    if (this.activeFilter === 'FAILED') {
      return this.importResult.records.filter(r => r.status !== 'READY');
    }
    return this.importResult.records;
  }

  generateBankFile(type: 'SAME_BANK' | 'OTHER_BANK' | 'ALL') {
    if (!this.importResult || this.importResult.totalSuccess === 0) return;

    this.isGenerating = true;

    const payload = {
      records: this.importResult.records,
      paymentDate: this.paymentDate,
      companyAccount: this.companyAccount,
      branchCode: this.branchCode,
      companyName: this.companyName,
      paymentType: type
    };

    this.http.post(`${environment.apiUrl}/bank-pay/generate`, payload, { responseType: 'text' }).subscribe({
      next: (txtContent) => {
        let fileName = 'sbi_bulk_payment.txt';
        if (type === 'SAME_BANK') fileName = 'sbi_same_bank_payment.txt';
        if (type === 'OTHER_BANK') fileName = 'sbi_other_bank_payment.txt';

        const blob = new Blob([txtContent], { type: 'text/plain;charset=utf-8' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);

        this.isGenerating = false;
      },
      error: (err) => {
        console.error('Generate error', err);
        alert('Failed to generate Bank Pay file.');
        this.isGenerating = false;
      }
    });
  }

  downloadExceptionReport() {
    if (!this.importResult || !this.importResult.records) return;

    const failedRecords = this.importResult.records.filter(r => r.status !== 'READY');
    if (failedRecords.length === 0) return;

    this.http.post(`${environment.apiUrl}/bank-pay/exception-report`, failedRecords, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'exception_report.xlsx';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Failed to download Exception Report', err);
        alert('Failed to generate Exception Report Excel.');
      }
    });
  }

  downloadSampleTemplate() {
    const csvContent = 'Employee Name,Amount\nJohn Doe,25000\nJane Smith,18000\n';
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'bank_pay_template.csv';
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
