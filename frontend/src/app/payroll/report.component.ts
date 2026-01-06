import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PayrollService, PayrollPeriod } from './payroll.service';
import { DialogService } from '../core/services/dialog.service';
import { DocumentService, UploadDocument } from '../core/services/document.service';
import { environment } from '../../environments/environment';


@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container mt-4">
      
      <div class="card shadow">
        <div class="card-header bg-secondary text-white">
          <h5 class="mb-0">Select Payroll Period</h5>
        </div>
        <div class="card-body">
          <div *ngIf="loading" class="text-center py-3">
             <span class="spinner-border spinner-border-sm text-primary"></span> Loading periods...
          </div>

          <div *ngIf="!loading && periods.length === 0" class="alert alert-warning">
             No payroll periods found. Please create one in Dashboard first.
          </div>

          <select *ngIf="!loading && periods.length > 0" class="form-select mb-3" [(ngModel)]="selectedPeriodId" (change)="loadUploadedDocs()">
            <option *ngFor="let p of periods" [value]="p.id">
               {{ getMonthName(p.month) }} {{ p.year }} ({{ p.status }})
            </option>
          </select>

          <hr>

          <div class="row g-4" [class.opacity-50]="!selectedPeriodId">
            <!-- PDF Reports -->
            <div class="col-md-6">
              <h5 class="text-primary">PDF Reports</h5>
              <div class="d-grid gap-2">
                <button class="btn btn-outline-danger text-start" (click)="download('main-file')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-pdf"></i> Main File (Payroll Engine)
                </button>
                <button class="btn btn-outline-danger text-start" (click)="download('payment-details')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-pdf"></i> Payment Details (Bank)
                </button>
                <button class="btn btn-outline-danger text-start" (click)="download('wage-summary')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-pdf"></i> Wage Summary
                </button>
                <button class="btn btn-outline-danger text-start" (click)="download('attendance')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-pdf"></i> Attendance Register
                </button>
              </div>

              <!-- UTR Import Section -->
              <div class="mt-4 border-top pt-3">
                  <h6 class="text-muted fw-bold"><i class="bi bi-bank"></i> Upload Bank Statement (UTR)</h6>
                  <div class="input-group input-group-sm mt-2">
                      <input type="file" class="form-control" (change)="onUtrFileSelected($event)" accept=".xlsx, .xls" [disabled]="!selectedPeriodId">
                      <button class="btn btn-outline-primary" (click)="uploadUtr()" [disabled]="!selectedPeriodId || !utrFile">
                          <i class="bi bi-upload"></i> Import
                      </button>
                  </div>
                  <small class="text-muted" *ngIf="utrUploadMsg" [class.text-danger]="utrUploadError" [class.text-success]="!utrUploadError">
                      {{ utrUploadMsg }}
                  </small>
              </div>
            </div>

            <!-- Excel/CSV Reports -->
            <div class="col-md-6">
              <h5 class="text-success">Compliance Returns</h5>
              <div class="d-grid gap-2">
                <button class="btn btn-outline-success text-start" (click)="download('esi')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-excel"></i> ESI Return (.xls)
                </button>
                <button class="btn btn-outline-dark text-start" (click)="download('epf')" [disabled]="!selectedPeriodId">
                  <i class="bi bi-file-text"></i> EPF Return (.txt)
                </button>
                
                <div class="mt-3 border-top pt-2">
                    <label class="form-label small fw-bold text-muted">Bulk Payment Date</label>
                    <div class="input-group">
                        <input type="date" class="form-control form-control-sm" [(ngModel)]="bulkDate">
                        <button class="btn btn-outline-secondary btn-sm" (click)="download('bulk')" [disabled]="!selectedPeriodId">
                            <i class="bi bi-file-text"></i> Download (.txt)
                        </button>
                    </div>
                </div>

              </div>
            </div>
          </div>

          <!-- Uploaded Documents Section -->
          <div class="card mt-4 shadow-sm border-0">
              <div class="card-header bg-info text-white">
                  <h5 class="mb-0">EPF & ESI Uploaded Documents</h5>
              </div>
              <div class="card-body p-0">
                  <div class="table-responsive">
                      <table class="table table-hover mb-0">
                          <thead class="table-light">
                              <tr>
                                  <th>Date</th>
                                  <th>Type</th>
                                  <th>Sub Type</th>
                                  <th>Filename</th>
                                  <th>Action</th>
                              </tr>
                          </thead>
                          <tbody>
                              <tr *ngFor="let doc of uploadedDocs">
                                  <td>{{ doc.uploadDate | date:'mediumDate' }}</td>
                                  <td><span class="badge" [class.bg-primary]="doc.type === 'ESI'" [class.bg-success]="doc.type === 'EPF'">{{ doc.type }}</span></td>
                                  <td>{{ doc.subType }}</td>
                                  <td>{{ doc.fileName }}</td>
                                  <td>
                                      <button class="btn btn-sm btn-outline-primary" (click)="downloadDoc(doc.id)">
                                          <i class="bi bi-download"></i> Download
                                      </button>
                                  </td>
                              </tr>
                              <tr *ngIf="uploadedDocs.length === 0">
                                  <td colspan="5" class="text-center text-muted py-3">No documents uploaded.</td>
                              </tr>
                          </tbody>
                      </table>
                  </div>
              </div>
          </div>

        </div>
      </div>
    </div>
  `
})
export class ReportComponent implements OnInit {
  selectedPeriodId: string | null = null;
  periods: PayrollPeriod[] = [];
  loading = true;
  bulkDate: string = new Date().toISOString().split('T')[0];

  constructor(
    private http: HttpClient,
    private router: Router,
    private payrollService: PayrollService,
    private dialogService: DialogService,
    private documentService: DocumentService
  ) { }

  ngOnInit() {
    this.payrollService.getPeriods().subscribe({
      next: (data) => {
        this.periods = data;

        // CHECK GLOBAL SELECTION
        const globalRef = this.payrollService.getGlobalSelection();
        if (globalRef) {
          const found = this.periods.find(p => p.month === globalRef.month && p.year === globalRef.year);
          if (found) {
            this.selectedPeriodId = found.id;
          } else {
            // Warn or default? Default to latest if not found, or show empty.
            this.selectedPeriodId = this.periods.length > 0 ? this.periods[0].id : null;
          }
        } else if (this.periods.length > 0) {
          this.selectedPeriodId = this.periods[0].id; // Default to latest
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load periods', err);
        this.loading = false;
      }
    });

    this.loadUploadedDocs();
  }

  uploadedDocs: UploadDocument[] = [];
  loadUploadedDocs() {
    if (!this.selectedPeriodId) return;
    this.documentService.getDocuments(this.selectedPeriodId).subscribe({
      next: (docs) => this.uploadedDocs = docs,
      error: (err) => console.error('Failed to load docs', err)
    });
  }

  downloadDoc(id: string) {
    this.documentService.downloadFile(id);
  }

  getMonthName(month: number): string {
    const date = new Date();
    date.setMonth(month - 1);
    return date.toLocaleString('default', { month: 'long' });
  }

  download(type: string) {
    let url = `${environment.apiUrl}/reports/${this.selectedPeriodId}/${type}`;

    // Append bulkDate if type is bulk
    if (type === 'bulk') {
      url += `?paymentDate=${this.bulkDate}`;
    }

    // Use HttpClient to secure the request (Interceptor adds Token)
    this.http.get(url, { responseType: 'blob', observe: 'response' }).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.dialogService.alert('Error', 'File not found or empty response.');
          return;
        }

        // Create a temporary link to trigger download
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;

        // Try to guess filename from header or type
        let filename = `${type}_report.pdf`;
        if (type === 'esi') filename = 'esi_return.xls';
        if (type === 'epf') filename = 'epf_return.txt';
        if (type === 'bulk') {
          const p = this.periods.find(p => p.id === this.selectedPeriodId);
          if (p) {
            const m = this.getMonthName(p.month).substring(0, 3).toLowerCase();
            filename = `${m}_bulk_payment.txt`;
          } else {
            filename = 'bulk_payment.txt';
          }
        }

        const contentDisposition = response.headers.get('Content-Disposition');
        if (contentDisposition) {
          const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
          if (matches != null && matches[1]) {
            filename = matches[1].replace(/['"]/g, '');
          }
        }

        link.download = filename;
        link.click();

        // Cleanup
        window.URL.revokeObjectURL(downloadUrl);
      },
      error: (err) => {
        console.error(err);
        this.dialogService.alert('Error', 'Download failed! ' + (err.statusText || 'Server Error'));
      }
    });
  }

  // --- UTR IMPORT LOGIC ---
  utrFile: File | null = null;
  utrUploadMsg: string | null = null;
  utrUploadError = false;

  onUtrFileSelected(event: any) {
    if (event.target.files.length > 0) {
      this.utrFile = event.target.files[0];
      this.utrUploadMsg = null;
    }
  }

  uploadUtr() {
    if (!this.utrFile || !this.selectedPeriodId) return;

    this.utrUploadMsg = "Uploading...";
    this.utrUploadError = false;

    this.payrollService.importUtr(this.utrFile, this.selectedPeriodId).subscribe({
      next: (res: any) => {
        this.utrUploadMsg = res.message || "Import Successful.";
        this.utrFile = null;
        // Reset file input? (Optional, requires ViewChild or direct DOM, skip for now)
      },
      error: (err) => {
        console.error(err);
        this.utrUploadError = true;
        this.utrUploadMsg = err.error?.message || "Import Failed.";
      }
    });
  }
}
