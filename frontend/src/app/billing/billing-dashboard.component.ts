import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { environment } from '../../environments/environment';


interface IssueSlipDTO {
    siNo: string;
    entryDate: string;
    slipNumber: string;
    totalBags: number;
    status: 'EXTRACTED' | 'NEEDS_VERIFICATION' | 'EDITED';
    warningMessage?: string;
}

@Component({
    selector: 'app-billing-dashboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <!-- DASHBOARD VIEW -->
    <div *ngIf="activeModule === 'DASHBOARD'" class="billing-container d-flex flex-column align-items-center justify-content-center p-4">
      
      <div class="header mb-5 text-center">
        <h1 class="fw-bold text-dark mb-2">Billing Operations</h1>
        <p class="text-muted">Select a module to begin</p>
      </div>

      <!-- Modules Stack -->
      <div class="d-flex flex-column gap-4 w-100" style="max-width: 400px;">
        
        <!-- Receipt Module -->
        <div class="card module-card border-0 shadow-sm p-4" (click)="selectModule('RECEIPT')">
            <div class="d-flex align-items-center">
                <div class="icon-circle bg-success-subtle text-success me-4">
                    <i class="bi bi-receipt fs-4"></i>
                </div>
                <div>
                    <h3 class="h5 fw-bold mb-1">Receipt Module</h3>
                    <small class="text-muted">Process height-wise intake slips</small>
                </div>
                <i class="bi bi-chevron-right ms-auto text-muted"></i>
            </div>
        </div>

        <!-- Issue Module -->
        <div class="card module-card border-0 shadow-sm p-4" (click)="selectModule('ISSUE')">
            <div class="d-flex align-items-center">
                <div class="icon-circle bg-primary-subtle text-primary me-4">
                    <i class="bi bi-box-seam fs-4"></i>
                </div>
                <div>
                    <h3 class="h5 fw-bold mb-1">Issue Module</h3>
                    <small class="text-muted">Process issue bags & rates</small>
                </div>
                <i class="bi bi-chevron-right ms-auto text-muted"></i>
            </div>
        </div>

        <!-- QC Module -->
        <div class="card module-card border-0 shadow-sm p-4" (click)="selectModule('QC')">
            <div class="d-flex align-items-center">
                <div class="icon-circle bg-warning-subtle text-warning me-4">
                    <i class="bi bi-person-gear fs-4"></i>
                </div>
                <div>
                    <h3 class="h5 fw-bold mb-1">QC Module</h3>
                    <small class="text-muted">Casual labour & wage management</small>
                </div>
                <i class="bi bi-chevron-right ms-auto text-muted"></i>
            </div>
        </div>
      
        <!-- Bill Generator -->
        <div class="card module-card border-0 shadow-sm p-4 mt-2 bg-dark text-white" (click)="selectModule('BILL')">
            <div class="d-flex align-items-center justify-content-center">
                <i class="bi bi-file-earmark-pdf fs-4 me-3"></i>
                <h3 class="h5 fw-bold mb-0">Generate Monthly Bill</h3>
            </div>
        </div>

      </div>
    </div>

    <!-- ISSUE MODULE VIEW -->
    <div *ngIf="activeModule === 'ISSUE'" class="container mt-4">
        <div class="d-flex align-items-center mb-4">
            <button class="btn btn-outline-secondary me-3" (click)="activeModule = 'DASHBOARD'">
                <i class="bi bi-arrow-left"></i> Back
            </button>
            <h2 class="fw-bold mb-0">Issue Module (Bill No 23 Issue)</h2>
        </div>

        <!-- UPLOAD SECTION (Initial State) -->
        <div *ngIf="issueSlips.length === 0" class="upload-area p-5 text-center border rounded bg-white shadow-sm">
            <div class="mb-3 text-primary">
                <i class="bi bi-cloud-upload display-4"></i>
            </div>
            <h3 class="fw-bold">Upload work slips only for Issue</h3>
            <p class="text-muted mb-4">Select multiple images (JPG, PNG) to begin extraction.</p>
            
            <input type="file" multiple (change)="onIssueFilesSelected($event)" id="issueFileInput" class="d-none">
            <label for="issueFileInput" class="btn btn-primary px-4 py-2">
                <span *ngIf="!isExtracting">Select Photos</span>
                <span *ngIf="isExtracting">Processing...</span>
            </label>
        </div>

        <!-- VERIFICATION TABLE (Post-Extraction) -->
        <div *ngIf="issueSlips.length > 0" class="card border-0 shadow-sm">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold">Verification Table</h5>
                 <div>
                    <button class="btn btn-outline-danger me-2" (click)="issueSlips = []">Discard All</button>
                    <button class="btn btn-success" [disabled]="hasVerificationErrors()" (click)="saveIssueData()">
                        Confirm & Save
                    </button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="bg-light">
                        <tr>
                            <th style="width: 5%">SI No</th>
                            <th style="width: 15%">Date of Operation</th>
                            <th style="width: 20%">Work Slip No</th>
                            <th style="width: 15%">Clause XIX – Part 1(5)</th>
                            <th style="width: 20%">Status</th>
                            <th style="width: 5%">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let row of issueSlips" [class.table-danger]="row.status === 'NEEDS_VERIFICATION'">
                            <td>{{ row.siNo }}</td>
                            <td>
                                <input type="date" [(ngModel)]="row.entryDate" (change)="markEdited(row)" class="form-control form-control-sm">
                            </td>
                            <td>
                                <input type="text" [(ngModel)]="row.slipNumber" (change)="markEdited(row)" class="form-control form-control-sm" placeholder="SLIP-XXXX">
                            </td>
                            <td>
                                <input type="number" [(ngModel)]="row.totalBags" (change)="markEdited(row)" class="form-control form-control-sm" placeholder="0">
                            </td>
                            <td>
                                <span class="badge" 
                                    [class.bg-success]="row.status === 'EXTRACTED'"
                                    [class.bg-danger]="row.status === 'NEEDS_VERIFICATION'"
                                    [class.bg-primary]="row.status === 'EDITED'">
                                    {{ row.status.replace('_', ' ') }}
                                </span>
                                <div *ngIf="row.warningMessage" class="text-danger small mt-1 fw-bold">
                                    <i class="bi bi-exclamation-triangle-fill"></i> {{ row.warningMessage }}
                                </div>
                            </td>
                            <td>
                                <button class="btn btn-sm btn-light text-danger" (click)="removeRow(row)">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div class="card-footer bg-light" *ngIf="hasVerificationErrors()">
                 <div class="text-danger fw-bold">
                    <i class="bi bi-shield-lock-fill me-2"></i>
                    Some records require verification. Please review highlighted rows before saving.
                 </div>
            </div>
        </div>

        <!-- SAVED RECORDS (VIEW ONLY) -->
        <div class="mt-5">
            <h4 class="fw-bold mb-3 text-secondary">Saved Records (Bill No 23 Issue)</h4>
            <div class="card border-0 shadow-sm" *ngIf="savedIssueSlips.length > 0; else noSavedData">
                <div class="table-responsive">
                    <table class="table table-striped align-middle mb-0">
                        <thead class="bg-light">
                            <tr>
                                <th>Slip No</th>
                                <th>Date</th>
                                <th>Clause XIX – Part 1(5) (Bags)</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr *ngFor="let slip of savedIssueSlips">
                                <td class="fw-bold">{{ slip.slipNumber }}</td>
                                <td>{{ slip.entryDate }}</td>
                                <td>{{ slip.issueTotalBags }}</td>
                                <td><span class="badge bg-success">SAVED</span></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <ng-template #noSavedData>
                <div class="alert alert-light text-center border">
                    No saved records found for this module.
                </div>
            </ng-template>
        </div>

    </div>
  `,
    styles: [`
    .billing-container { min-height: 85vh; background: #f8f9fa; }
    .module-card { 
        cursor: pointer; 
        transition: all 0.3s ease;
        border-radius: 12px;
    }
    .module-card:hover { 
        transform: translateY(-5px); 
        box-shadow: 0 10px 20px rgba(0,0,0,0.08) !important;
    }
    .icon-circle {
        width: 50px; height: 50px;
        border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
    }
    .upload-area {
        border-style: dashed !important;
        border-width: 2px !important;
        border-color: #dee2e6 !important;
    }
  `]
})
export class BillingDashboardComponent implements OnInit {
    activeModule: 'DASHBOARD' | 'ISSUE' | 'RECEIPT' | 'QC' | 'BILL' = 'DASHBOARD';
    isExtracting = false;

    // Issue Module Data
    issueSlips: IssueSlipDTO[] = [];
    savedIssueSlips: any[] = []; // Store saved records

    constructor(
        private authService: AuthService,
        private router: Router,
        private route: ActivatedRoute,
        private http: HttpClient
    ) { }

    ngOnInit() {
        // Subscribe to query params to switch modules
        this.route.queryParams.subscribe(params => {
            const module = params['module'];
            if (module) {
                this.activeModule = module.toUpperCase() as any;
                if (this.activeModule === 'ISSUE') {
                    this.loadSavedIssueSlips();
                }
            } else {
                this.activeModule = 'DASHBOARD';
            }
        });
    }

    selectModule(module: string) {
        // Update URL when selecting from Dashboard cards
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { module: module },
            queryParamsHandling: 'merge'
        });
    }

    // --- ISSUE MODULE LOGIC ---

    loadSavedIssueSlips() {
        this.http.get<any[]>(`${environment.apiUrl}/billing/issue/list`)
            .subscribe({
                next: (data) => this.savedIssueSlips = data,
                error: (err) => console.error("Failed to load saved slips", err)
            });
    }

    onIssueFilesSelected(event: any) {
        const files: FileList = event.target.files;
        if (!files || files.length === 0) return;

        this.isExtracting = true;
        const formData = new FormData();
        Array.from(files).forEach(file => {
            formData.append('files', file);
        });

        this.http.post<IssueSlipDTO[]>(`${environment.apiUrl}/billing/issue/extract`, formData)
            .subscribe({
                next: (data) => {
                    this.issueSlips = data;
                    this.isExtracting = false;
                },
                error: (err) => {
                    alert("Extraction Failed: " + err.message);
                    this.isExtracting = false;
                }
            });
    }

    markEdited(row: IssueSlipDTO) {
        row.status = 'EDITED';
        row.warningMessage = undefined; // Clear warning if user edits
    }

    removeRow(row: IssueSlipDTO) {
        this.issueSlips = this.issueSlips.filter(r => r !== row);
    }

    hasVerificationErrors(): boolean {
        return this.issueSlips.some(r => r.status === 'NEEDS_VERIFICATION' || !r.slipNumber || !r.entryDate || !r.totalBags);
    }

    saveIssueData() {
        if (this.hasVerificationErrors()) return;

        this.http.post(`${environment.apiUrl}/billing/issue/save`, this.issueSlips)
            .subscribe({
                next: () => {
                    alert("Success: All Issue slips saved to Bill No 23 Issue.");
                    this.issueSlips = [];
                    this.loadSavedIssueSlips(); // Refresh saved list
                },
                error: (err) => {
                    alert("Save Failed: " + (err.error?.message || err.message));
                }
            });
    }
}
