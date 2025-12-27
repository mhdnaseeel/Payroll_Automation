import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService, UploadDocument } from '../core/services/document.service';
import { PayrollService, PayrollPeriod } from '../payroll/payroll.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container mt-4">
      <h2>Upload Documents</h2>
      <hr>

      <div class="row">
        <!-- Upload Form -->
        <div class="col-md-5">
          <div class="card shadow-sm">
            <div class="card-header bg-primary text-white">
              <h5 class="mb-0">New Upload</h5>
            </div>
            <div class="card-body">
              <!-- Period Selector -->
              <div class="mb-3">
                <label class="form-label">Payroll Period</label>
                <select class="form-select" [(ngModel)]="selectedPeriodId" (change)="onPeriodChange()" [disabled]="loadingPeriods">
                  <option *ngFor="let p of periods" [value]="p.id">
                    {{ getMonthName(p.month) }} {{ p.year }} ({{ p.status }})
                  </option>
                </select>
                <small *ngIf="loadingPeriods" class="text-muted">Loading periods...</small>
              </div>

              <div class="mb-3">
                <label class="form-label">Type</label>
                <select class="form-select" [(ngModel)]="selectedType" (change)="onTypeChange()">
                  <option value="ESI">ESI</option>
                  <option value="EPF">EPF</option>
                </select>
              </div>

              <div class="mb-3">
                <label class="form-label">Sub Type</label>
                <select class="form-select" [(ngModel)]="selectedSubType" (change)="onSubTypeChange()">
                  <option *ngFor="let st of availableSubTypes" [value]="st">{{ st }}</option>
                </select>
              </div>

              <!-- EXISTING FILE WARNING -->
              <div *ngIf="existingDocument && !isReplacing" class="alert alert-warning">
                  <h6 class="alert-heading"><i class="bi bi-exclamation-triangle"></i> File Exists</h6>
                  <p class="mb-0 small">"{{ existingDocument.fileName }}" is already uploaded.</p>
                  <hr>
                  <button class="btn btn-sm btn-outline-dark w-100" (click)="enableReplacement()">Replace File</button>
              </div>

              <div class="mb-3" *ngIf="!existingDocument || isReplacing">
                <label class="form-label">File (PDF Only)</label>
                <input type="file" class="form-control" (change)="onFileSelected($event)" accept="application/pdf">
                <small *ngIf="fileError" class="text-danger">{{ fileError }}</small>
                <div *ngIf="isReplacing" class="mt-2 text-end">
                    <button class="btn btn-sm btn-link text-muted" (click)="cancelReplacement()">Cancel Replace</button>
                </div>
              </div>

              <button *ngIf="!existingDocument || isReplacing" class="btn w-100" [class.btn-success]="!isReplacing" [class.btn-warning]="isReplacing" (click)="upload()" [disabled]="!canUpload()">
                <i class="bi" [class.bi-cloud-upload]="!isReplacing" [class.bi-arrow-repeat]="isReplacing"></i> 
                {{ isReplacing ? 'Upload & Replace' : 'Upload' }}
              </button>

              <div *ngIf="message" class="mt-3 alert" [class.alert-success]="!isError" [class.alert-danger]="isError">
                {{ message }}
              </div>
            </div>
          </div>
        </div>

        <!-- Document List -->
        <div class="col-md-7">
          <div class="card shadow-sm">
            <div class="card-header bg-light">
              <h5 class="mb-0">Uploaded Documents</h5>
            </div>
            <div class="card-body p-0">
               <div class="list-group list-group-flush" *ngIf="documents.length > 0; else noDocs">
                  <div class="list-group-item d-flex justify-content-between align-items-center" *ngFor="let doc of documents">
                     <div>
                        <h6 class="mb-0">{{ doc.type }} - {{ doc.subType }}</h6>
                        <small class="text-muted">{{ doc.fileName }}</small><br>
                        <small class="text-muted" style="font-size: 0.75rem;">{{ doc.uploadDate | date:'medium' }}</small>
                     </div>
                     <button class="btn btn-sm btn-outline-primary" (click)="download(doc.id)">
                        <i class="bi bi-download"></i>
                     </button>
                  </div>
               </div>
               <ng-template #noDocs>
                  <div class="p-4 text-center text-muted">
                     No documents uploaded yet.
                  </div>
               </ng-template>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class UploadComponent implements OnInit {
  selectedType: string = 'ESI';
  selectedSubType: string = 'Contribution Report';
  availableSubTypes: string[] = ['Contribution Report', 'ESIC'];

  selectedFile: File | null = null;
  fileError: string | null = null;

  message: string | null = null;
  isError: boolean = false;

  documents: UploadDocument[] = [];

  // Period Selection
  periods: PayrollPeriod[] = [];
  selectedPeriodId: string | null = null;
  loadingPeriods = true;

  constructor(private documentService: DocumentService, private payrollService: PayrollService) { }

  ngOnInit() {
    this.loadPeriods();
  }

  loadPeriods() {
    this.payrollService.getPeriods().subscribe({
      next: (data) => {
        this.periods = data;
        this.loadingPeriods = false;

        // Default to first or global
        const globalRef = this.payrollService.getGlobalSelection();
        if (globalRef) {
          const found = this.periods.find(p => p.month === globalRef.month && p.year === globalRef.year);
          if (found) {
            this.selectedPeriodId = found.id;
          } else if (this.periods.length > 0) {
            this.selectedPeriodId = this.periods[0].id;
          }
        } else if (this.periods.length > 0) {
          this.selectedPeriodId = this.periods[0].id;
        }

        if (this.selectedPeriodId) {
          this.loadDocuments();
        }
      },
      error: (err) => {
        console.error('Failed to load periods', err);
        this.loadingPeriods = false;
      }
    });
  }

  onPeriodChange() {
    if (this.selectedPeriodId) {
      this.loadDocuments();
    }
  }

  getMonthName(month: number): string {
    const date = new Date();
    date.setMonth(month - 1);
    return date.toLocaleString('default', { month: 'long' });
  }

  existingDocument: UploadDocument | null = null;
  isReplacing: boolean = false;

  checkExisting() {
    this.existingDocument = this.documents.find(d =>
      d.type === this.selectedType &&
      d.subType === this.selectedSubType
    ) || null;
    this.isReplacing = false;
    this.selectedFile = null;
    this.message = null;
  }

  enableReplacement() {
    this.isReplacing = true;
  }

  cancelReplacement() {
    this.isReplacing = false;
    this.selectedFile = null;
  }

  onTypeChange() {
    if (this.selectedType === 'ESI') {
      this.availableSubTypes = ['Contribution Report', 'ESIC'];
      this.selectedSubType = 'Contribution Report';
    } else {
      this.availableSubTypes = ['ECR', 'Payment Receipt'];
      this.selectedSubType = 'ECR';
    }
    this.checkExisting();
  }

  // Also call checkExisting on SubType change manually via template binding or direct method
  onSubTypeChange() {
    this.checkExisting();
  }

  onFileSelected(event: any) {
    this.fileError = null;
    const file = event.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        this.fileError = 'Only PDF files are allowed.';
        this.selectedFile = null;
        return;
      }
      this.selectedFile = file;
    }
  }

  canUpload(): boolean {
    // If replacing, we need a file. 
    // If not replacing but existing doc present, we cannot upload (unless we click replace).
    if (this.existingDocument && !this.isReplacing) return false;

    return !!this.selectedFile && !this.fileError && !!this.selectedType && !!this.selectedSubType && !!this.selectedPeriodId;
  }

  upload() {
    if (!this.canUpload()) return;

    this.isError = false;
    this.message = 'Uploading...';

    this.documentService.uploadFile(this.selectedFile!, this.selectedType, this.selectedSubType, this.selectedPeriodId!)
      .subscribe({
        next: (doc) => {
          this.message = 'Upload Successful!';
          this.selectedFile = null;
          // clear file input
          const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
          if (fileInput) fileInput.value = '';

          this.loadDocuments(); // This will refresh and re-trigger check via subscribe

          // Clear message after 3 seconds
          setTimeout(() => this.message = null, 3000);
        },
        error: (err) => {
          this.isError = true;
          this.message = 'Upload Failed. ' + (err.error?.message || err.statusText);
        }
      });
  }

  loadDocuments() {
    if (!this.selectedPeriodId) return;
    this.documentService.getDocuments(this.selectedPeriodId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.checkExisting();
      },
      error: (err) => console.error('Failed to load documents', err)
    });
  }

  download(id: string) {
    this.documentService.downloadFile(id);
  }
}
