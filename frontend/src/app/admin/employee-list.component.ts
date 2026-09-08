import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../core/services/dialog.service';
import { environment } from '../../environments/environment';

interface Employee {
  id?: string;
  memberId: string;
  fullName: string;
  uanNumber: string;
  ipNumber: string;
  bankAccountNo: string;
  ifscCode: string;
  status: 'ACTIVE' | 'INACTIVE';
  category: 'CL' | 'HL';
  inactiveDate?: string; // YYYY-MM-DD
}

interface ImportFailureDetail {
  rowNumber: number;
  memberId?: string;
  name?: string;
  reason: string;
}

interface ImportUpdateDetail {
  rowNumber: number;
  memberId?: string;
  name?: string;
  changes: { field: string; oldValue: string; newValue: string; }[];
}

interface EmployeeImportSummary {
  totalRecords: number;
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  failedRecords: ImportFailureDetail[];
  updatedRecords: ImportUpdateDetail[];
}

interface NewEmployeePreview {
  rowNumber: number;
  memberId: string;
  fullName: string;
  uanNumber?: string;
  ipNumber?: string;
  bankAccountNo?: string;
  ifscCode?: string;
  category?: string;
}

interface UpdateEmployeePreview {
  rowNumber: number;
  memberId: string;
  fullName: string;
  changes: { field: string; oldValue: string; newValue: string; }[];
}

interface ImportPreviewResult {
  totalRows: number;
  toCreate: NewEmployeePreview[];
  toUpdate: UpdateEmployeePreview[];
  skippedCount: number;
  failed: ImportFailureDetail[];
}

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container mt-4">
      <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3 gap-md-0">
        <h2>Employee Master (Admin)</h2>
        <div class="d-flex gap-2 w-100 w-md-auto justify-content-end flex-wrap">
            <input type="file" #fileInput (change)="onFileSelected($event)" style="display:none" accept=".xlsx, .xls">
            <button class="btn border-0 shadow-sm text-white" style="background-color: #2c5f2d; font-weight: 500;" (click)="fileInput.click()" [disabled]="isPreviewLoading || isApplying">
                <span *ngIf="isPreviewLoading" class="spinner-border spinner-border-sm me-1" role="status"></span>
                <i *ngIf="!isPreviewLoading" class="bi bi-upload"></i>
                {{ isPreviewLoading ? 'Scanning...' : 'Import Excel' }}
            </button>
            <button class="btn border-0 shadow-sm text-white" style="background-color: #0f2615;" (click)="openAddModal()">+ Add New</button>
            <button class="btn border-0 shadow-sm text-white" style="background-color: #8b2635; font-weight: 500;" (click)="openResetModal()">
                <i class="bi bi-trash"></i> Reset Database
            </button>
        </div>
      </div>

      <div class="card shadow">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-striped mb-0 text-nowrap">
            <thead class="text-white" style="background-color: #0f2615;">
              <tr>
                <th class="py-3 ps-3 rounded-top-left" style="border-bottom: 3px solid #1e4d2b;">Member ID</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">Name</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">Category</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">UAN</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">IP Number</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">Bank A/c</th>
                <th class="py-3" style="border-bottom: 3px solid #1e4d2b;">Status</th>
                <th class="py-3 rounded-top-right" style="border-bottom: 3px solid #1e4d2b;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let emp of employees">
                <td>{{ emp.memberId }}</td>
                <td>{{ emp.fullName }}</td>
                <td><span class="badge text-white" [style.background-color]="emp.category === 'HL' ? '#2c3e50' : '#7f8c8d'">{{ emp.category }}</span></td>
                <td>{{ emp.uanNumber }}</td>
                <td>{{ emp.ipNumber }}</td>
                <td>{{ emp.bankAccountNo }}</td>
                <td>
                  <span class="badge" [ngClass]="emp.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary'">
                    {{ emp.status }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-sm btn-outline-primary me-2" (click)="openEditModal(emp)">Edit</button>
                  <button class="btn btn-sm btn-outline-danger" (click)="deleteEmployee(emp.id!)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
          </div>
        </div>
      </div>
      
      <!-- New/Edit Employee Modal -->
      <div *ngIf="showModal" class="modal d-block" style="background: rgba(0,0,0,0.5)">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ isEditMode ? 'Edit' : 'Add' }} Employee</h5>
              <button type="button" class="btn-close" (click)="showModal = false"></button>
            </div>
            <div class="modal-body">
               <form>
                  <div class="mb-2">
                      <label>Member ID</label>
                      <input type="text" class="form-control" [(ngModel)]="currentEmployee.memberId" name="memberId">
                  </div>
                  <div class="mb-2">
                      <label>Full Name</label>
                      <input type="text" class="form-control" [(ngModel)]="currentEmployee.fullName" name="fullName">
                  </div>
                  <div class="row">
                      <div class="col mb-2">
                          <label>UAN</label>
                          <input type="text" class="form-control" [(ngModel)]="currentEmployee.uanNumber" name="uan"
                                 (keypress)="onlyNumbers($event)" maxlength="12" placeholder="Numbers only">
                      </div>
                      <div class="col mb-2">
                          <label>IP Number</label>
                          <input type="text" class="form-control" [(ngModel)]="currentEmployee.ipNumber" name="ip"
                                 (keypress)="onlyNumbers($event)" placeholder="Numbers only">
                      </div>
                  </div>
                  <div class="row">
                      <div class="col mb-2">
                          <label>Bank A/c</label>
                          <input type="text" class="form-control" [(ngModel)]="currentEmployee.bankAccountNo" name="bank"
                                 (keypress)="onlyNumbers($event)" placeholder="Numbers only">
                      </div>
                      <div class="col mb-2">
                          <label>IFSC</label>
                          <input type="text" class="form-control" [(ngModel)]="currentEmployee.ifscCode" name="ifsc">
                      </div>
                  </div>
                  <div class="mb-2">
                      <label>Status</label>
                      <select class="form-select" [(ngModel)]="currentEmployee.status" name="status">
                          <option value="ACTIVE">ACTIVE</option>
                          <option value="INACTIVE">INACTIVE</option>
                      </select>
                  </div>
                  <div class="mb-2">
                      <label>Inactive Date (Last Working Day)</label>
                      <input type="date" class="form-control" [(ngModel)]="currentEmployee.inactiveDate" name="inactiveDate">
                      <small class="text-muted">If set, employee will be excluded from payroll periods starting AFTER this date.</small>
                  </div>
                  <div class="mb-2">
                      <label>Category</label>
                      <select class="form-select" [(ngModel)]="currentEmployee.category" name="category">
                          <option value="CL">Casual Labour (CL)</option>
                          <option value="HL">Head Loader (HL)</option>
                      </select>
                  </div>
               </form>
            </div>
            <div class="modal-footer">
               <button class="btn btn-secondary" (click)="showModal = false">Close</button>
               <button class="btn btn-primary" (click)="saveEmployee()">Save Changes</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ─── Import Preview Modal ─────────────────────────────────────── -->
      <div *ngIf="showPreviewModal" class="modal d-block animate-fade-in" style="background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); z-index: 1050;">
        <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
          <div class="modal-content border-0 shadow-lg" style="border-radius: 12px; overflow: hidden;">
            <div class="modal-header text-white border-0" style="background: linear-gradient(135deg,#1a3a2a,#2c5f2d);">
              <h5 class="modal-title d-flex align-items-center gap-2">
                <i class="bi bi-eye-fill"></i> Import Preview — Review Before Applying
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="cancelPreview()"></button>
            </div>
            <div class="modal-body p-4" *ngIf="previewResult">

              <!-- Summary chips -->
              <div class="d-flex flex-wrap gap-3 mb-4">
                <span class="badge fs-6 px-3 py-2" style="background:#e8f5e9; color:#2c5f2d; border:1px solid #a5d6a7;">
                  <i class="bi bi-person-plus-fill me-1"></i> {{ previewResult.toCreate.length }} New
                </span>
                <span class="badge fs-6 px-3 py-2" style="background:#e3f2fd; color:#0d47a1; border:1px solid #90caf9;">
                  <i class="bi bi-pencil-fill me-1"></i> {{ previewResult.toUpdate.length }} Updates
                </span>
                <span class="badge fs-6 px-3 py-2" style="background:#f5f5f5; color:#555; border:1px solid #ddd;">
                  <i class="bi bi-skip-forward-fill me-1"></i> {{ previewResult.skippedCount }} Unchanged
                </span>
                <span *ngIf="previewResult.failed.length > 0" class="badge fs-6 px-3 py-2" style="background:#ffebee; color:#c62828; border:1px solid #ef9a9a;">
                  <i class="bi bi-exclamation-octagon-fill me-1"></i> {{ previewResult.failed.length }} Errors
                </span>
              </div>

              <!-- NEW EMPLOYEES section -->
              <div *ngIf="previewResult.toCreate.length > 0" class="mb-4">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <h6 class="fw-bold text-success mb-0 d-flex align-items-center gap-2">
                    <i class="bi bi-person-plus-fill"></i> New Employees to Add
                  </h6>
                  <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-outline-success py-0" (click)="selectAllNew(true)">Select All</button>
                    <button class="btn btn-sm btn-outline-secondary py-0" (click)="selectAllNew(false)">Deselect All</button>
                  </div>
                </div>
                <div class="table-responsive" style="max-height: 220px; overflow-y: auto;">
                  <table class="table table-sm table-hover border border-success-subtle mb-0">
                    <thead class="sticky-top" style="background-color:#e8f5e9;">
                      <tr>
                        <th style="width:40px;"><input type="checkbox" (change)="selectAllNew($any($event.target).checked)"></th>
                        <th style="width:55px;">Row</th>
                        <th style="width:90px;">Member ID</th>
                        <th>Name</th>
                        <th style="width:120px;">UAN</th>
                        <th style="width:110px;">IP Number</th>
                        <th style="width:80px;">Category</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let emp of previewResult.toCreate">
                        <td><input type="checkbox" [checked]="selectedNewMemberIds.has(emp.memberId)" (change)="toggleNew(emp.memberId, $any($event.target).checked)"></td>
                        <td class="fw-bold">{{ emp.rowNumber }}</td>
                        <td>{{ emp.memberId }}</td>
                        <td>{{ emp.fullName }}</td>
                        <td class="small text-muted">{{ emp.uanNumber || '-' }}</td>
                        <td class="small text-muted">{{ emp.ipNumber || '-' }}</td>
                        <td><span class="badge bg-success-subtle text-success">{{ emp.category || 'CL' }}</span></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <small class="text-muted">Uncheck any employee you do NOT want to add.</small>
              </div>

              <!-- UPDATES section -->
              <div *ngIf="previewResult.toUpdate.length > 0" class="mb-4">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <h6 class="fw-bold mb-0 d-flex align-items-center gap-2" style="color:#0d47a1;">
                    <i class="bi bi-pencil-fill"></i> Existing Employees with Changes
                  </h6>
                  <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-outline-primary py-0" (click)="selectAllUpdates(true)">Select All</button>
                    <button class="btn btn-sm btn-outline-secondary py-0" (click)="selectAllUpdates(false)">Deselect All</button>
                  </div>
                </div>
                <div class="table-responsive" style="max-height: 260px; overflow-y: auto;">
                  <table class="table table-sm table-hover border border-info-subtle mb-0">
                    <thead class="sticky-top" style="background-color:#e3f2fd;">
                      <tr>
                        <th style="width:40px;"><input type="checkbox" (change)="selectAllUpdates($any($event.target).checked)"></th>
                        <th style="width:55px;">Row</th>
                        <th style="width:90px;">Member ID</th>
                        <th style="width:140px;">Name</th>
                        <th style="width:140px;">Desired Action</th>
                        <th style="width:110px;">Field</th>
                        <th>Old Value</th>
                        <th>New Value</th>
                      </tr>
                    </thead>
                    <tbody>
                      <ng-container *ngFor="let upd of previewResult.toUpdate">
                        <tr *ngFor="let ch of upd.changes; let i = index">
                          <td>
                            <input *ngIf="i === 0" type="checkbox"
                              [checked]="selectedUpdateMemberIds.has(upd.memberId)"
                              (change)="toggleUpdate(upd.memberId, $any($event.target).checked)">
                          </td>
                          <td class="fw-bold">{{ i === 0 ? upd.rowNumber : '' }}</td>
                          <td>
                            <span *ngIf="i === 0">
                              <span [style.text-decoration]="addAsNewMemberIds.has(upd.memberId) ? 'line-through' : 'none'" class="text-muted">{{ upd.memberId }}</span>
                              <span *ngIf="addAsNewMemberIds.has(upd.memberId)" class="badge bg-success ms-1 small" title="Auto-assign next ID">Next ID</span>
                            </span>
                          </td>
                          <td>{{ i === 0 ? upd.fullName : '' }}</td>
                          <td>
                            <select *ngIf="i === 0" class="form-select form-select-sm py-0 border-info" style="font-size:0.8rem; height: 26px;"
                                    [value]="addAsNewMemberIds.has(upd.memberId) ? 'NEW' : 'UPDATE'"
                                    (change)="onChangeAction(upd.memberId, $any($event.target).value)">
                              <option value="UPDATE">Update Existing</option>
                              <option value="NEW">Add as New</option>
                            </select>
                          </td>
                          <td><span class="badge bg-info text-dark">{{ ch.field }}</span></td>
                          <td class="text-muted small" style="text-decoration:line-through;">{{ ch.oldValue || '-' }}</td>
                          <td class="text-success small fw-semibold">{{ ch.newValue || '-' }}</td>
                        </tr>
                      </ng-container>
                    </tbody>
                  </table>
                </div>
                <small class="text-muted">Choose "Update Existing" to replace the matched employee, or "Add as New" to create a new employee with a sequential Member ID.</small>
              </div>

              <!-- ERRORS section -->
              <div *ngIf="previewResult.failed.length > 0" class="mb-3">
                <h6 class="fw-bold text-danger mb-2"><i class="bi bi-exclamation-octagon-fill me-1"></i> Rows with Errors (will be skipped)</h6>
                <div class="table-responsive" style="max-height:160px; overflow-y:auto;">
                  <table class="table table-sm table-danger border mb-0">
                    <thead><tr><th>Row</th><th>Member ID</th><th>Name</th><th>Reason</th></tr></thead>
                    <tbody>
                      <tr *ngFor="let f of previewResult.failed">
                        <td>{{ f.rowNumber }}</td><td>{{ f.memberId || '-' }}</td>
                        <td>{{ f.name || '-' }}</td><td class="small">{{ f.reason }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- NOTHING TO DO -->
              <div *ngIf="previewResult.toCreate.length === 0 && previewResult.toUpdate.length === 0 && previewResult.failed.length === 0"
                   class="alert alert-secondary d-flex align-items-center gap-2">
                <i class="bi bi-check2-circle fs-5"></i>
                <div>All {{ previewResult.skippedCount }} records already exist with identical data. Nothing to import.</div>
              </div>

            </div>
            <div class="modal-footer border-0 d-flex justify-content-between align-items-center">
              <small class="text-muted">
                {{ selectedNewMemberIds.size + selectedUpdateMemberIds.size }} change(s) selected
              </small>
              <div class="d-flex gap-2">
                <button class="btn btn-secondary" (click)="cancelPreview()">Cancel</button>
                <button class="btn text-white border-0" style="background:#2c5f2d;"
                  [disabled]="(selectedNewMemberIds.size + selectedUpdateMemberIds.size) === 0 || isApplying"
                  (click)="confirmAndApply()">
                  <span *ngIf="isApplying" class="spinner-border spinner-border-sm me-1"></span>
                  <i *ngIf="!isApplying" class="bi bi-check-circle-fill me-1"></i>
                  {{ isApplying ? 'Applying...' : 'Confirm & Apply (' + (selectedNewMemberIds.size + selectedUpdateMemberIds.size) + ' changes)' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Employee Import Summary Modal -->
      <div *ngIf="showImportSummaryModal" class="modal d-block animate-fade-in" style="background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); z-index: 1050;">
        <div class="modal-dialog modal-lg modal-dialog-centered">
          <div class="modal-content border-0 shadow-lg" style="border-radius: 12px; overflow: hidden;">
            <div class="modal-header text-white border-0" style="background-color: #2c5f2d;">
              <h5 class="modal-title d-flex align-items-center gap-2">
                <i class="bi bi-file-earmark-spreadsheet-fill"></i> Employee Import Summary
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="showImportSummaryModal = false"></button>
            </div>
            <div class="modal-body p-4">
              <div *ngIf="importSummary">
                <div class="row g-3 mb-4 text-center">
                  <div class="col">
                    <div class="p-3 bg-light rounded border">
                      <div class="text-muted small fw-bold text-uppercase">Total</div>
                      <div class="fs-3 fw-bold text-dark">{{ importSummary.totalRecords }}</div>
                    </div>
                  </div>
                  <div class="col">
                    <div class="p-3 bg-success-subtle rounded border border-success-subtle text-success">
                      <div class="small fw-bold text-uppercase">Created</div>
                      <div class="fs-3 fw-bold">{{ importSummary.createdCount }}</div>
                    </div>
                  </div>
                  <div class="col">
                    <div class="p-3 bg-info-subtle rounded border border-info-subtle text-info-emphasis">
                      <div class="small fw-bold text-uppercase">Updated</div>
                      <div class="fs-3 fw-bold">{{ importSummary.updatedCount }}</div>
                    </div>
                  </div>
                  <div class="col">
                    <div class="p-3 bg-secondary-subtle rounded border border-secondary-subtle text-secondary">
                      <div class="small fw-bold text-uppercase">Skipped</div>
                      <div class="fs-3 fw-bold">{{ importSummary.skippedCount }}</div>
                    </div>
                  </div>
                  <div class="col">
                    <div class="p-3 bg-danger-subtle rounded border border-danger-subtle text-danger">
                      <div class="small fw-bold text-uppercase">Failed</div>
                      <div class="fs-3 fw-bold">{{ importSummary.failedCount }}</div>
                    </div>
                  </div>
                </div>

                <!-- Result summary messages -->
                <div class="mt-3 d-flex flex-column gap-2">

                  <div *ngIf="importSummary.createdCount > 0" class="alert alert-success d-flex align-items-center gap-2 mb-0 py-2">
                    <i class="bi bi-person-plus-fill fs-5"></i>
                    <div><strong>{{ importSummary.createdCount }}</strong> new employee(s) were created successfully.</div>
                  </div>

                  <div *ngIf="importSummary.updatedCount > 0" class="alert alert-info d-flex align-items-center gap-2 mb-0 py-2">
                    <i class="bi bi-pencil-square fs-5"></i>
                    <div><strong>{{ importSummary.updatedCount }}</strong> existing employee(s) were updated with new data.</div>
                  </div>

                  <!-- Updated Records Detail Table -->
                  <div *ngIf="importSummary.updatedRecords && importSummary.updatedRecords.length > 0" class="mt-1">
                    <div class="table-responsive" style="max-height: 240px; overflow-y: auto;">
                      <table class="table table-sm table-hover border border-info-subtle mb-0">
                        <thead class="sticky-top" style="background-color: #e3f2fd;">
                          <tr>
                            <th style="width: 60px;">Row</th>
                            <th style="width: 90px;">Member ID</th>
                            <th style="width: 140px;">Name</th>
                            <th style="width: 110px;">Field</th>
                            <th>Old Value</th>
                            <th>New Value</th>
                          </tr>
                        </thead>
                        <tbody>
                          <ng-container *ngFor="let upd of importSummary.updatedRecords">
                            <tr *ngFor="let ch of upd.changes; let i = index">
                              <td class="fw-bold">{{ i === 0 ? upd.rowNumber : '' }}</td>
                              <td>{{ i === 0 ? (upd.memberId || '-') : '' }}</td>
                              <td>{{ i === 0 ? (upd.name || '-') : '' }}</td>
                              <td><span class="badge bg-info text-dark">{{ ch.field }}</span></td>
                              <td class="text-muted small" style="text-decoration: line-through;">{{ ch.oldValue || '-' }}</td>
                              <td class="text-success small fw-semibold">{{ ch.newValue || '-' }}</td>
                            </tr>
                          </ng-container>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div *ngIf="importSummary.skippedCount > 0" class="alert alert-secondary d-flex align-items-center gap-2 mb-0 py-2">
                    <i class="bi bi-skip-forward-fill fs-5"></i>
                    <div><strong>{{ importSummary.skippedCount }}</strong> record(s) were skipped — already exist in the database with identical data.</div>
                  </div>

                  <div *ngIf="importSummary.failedCount > 0" class="mt-2">
                    <h6 class="text-danger fw-bold mb-3 d-flex align-items-center gap-2">
                      <i class="bi bi-exclamation-octagon-fill"></i> Failure Details ({{ importSummary.failedCount }} records)
                    </h6>
                    <div class="table-responsive" style="max-height: 250px; overflow-y: auto;">
                      <table class="table table-sm table-striped table-hover border">
                        <thead class="table-light sticky-top">
                          <tr>
                            <th style="width: 70px;">Row #</th>
                            <th style="width: 120px;">Member ID</th>
                            <th style="width: 160px;">Name</th>
                            <th>Reason for Failure</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr *ngFor="let fail of importSummary.failedRecords">
                            <td class="fw-bold">{{ fail.rowNumber }}</td>
                            <td>{{ fail.memberId || '-' }}</td>
                            <td>{{ fail.name || '-' }}</td>
                            <td class="text-danger small">{{ fail.reason }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div *ngIf="importSummary.createdCount === 0 && importSummary.updatedCount === 0 && importSummary.skippedCount === 0 && importSummary.failedCount === 0" class="alert alert-warning d-flex align-items-center gap-2 mb-0 py-2">
                    <i class="bi bi-exclamation-triangle-fill fs-5"></i>
                    <div>No records were processed. The file may be empty.</div>
                  </div>

                </div>
              </div>
            </div>
            <div class="modal-footer bg-light border-0">
              <button type="button" class="btn text-white px-4 py-2" style="background-color: #2c5f2d; border-radius: 6px;" (click)="showImportSummaryModal = false">
                Done
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Reset Database Modal with Captcha -->
      <div *ngIf="showResetModal" class="modal d-block animate-fade-in" style="background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); z-index: 1050;">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-0 shadow-lg" style="border-radius: 12px; overflow: hidden;">
            <div class="modal-header text-white border-0" style="background-color: #8b2635;">
              <h5 class="modal-title d-flex align-items-center gap-2">
                <i class="bi bi-exclamation-triangle-fill"></i> Reset Database (v2)
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="showResetModal = false"></button>
            </div>
            <div class="modal-body p-4 text-center">
              <p class="text-danger fw-bold mb-3">WARNING: This will permanently delete all employees, payroll entries, periods, and reset the database to a clean state. This action CANNOT be undone!</p>
              
              <div class="d-flex flex-column align-items-center mb-4 p-3 bg-light rounded border">
                <label class="form-label text-muted fw-semibold mb-2 small text-uppercase tracking-wider">Verification Captcha</label>
                <canvas #captchaCanvas width="180" height="60" class="border rounded mb-3 bg-white shadow-sm"></canvas>
                <button type="button" class="btn btn-sm btn-link text-decoration-none text-muted mb-2" (click)="generateCaptcha()">
                  <i class="bi bi-arrow-clockwise"></i> Regenerate Captcha
                </button>
                
                <div class="w-100 px-3">
                  <input type="text" class="form-control text-center fw-bold fs-4 py-2 border-2" 
                         [(ngModel)]="enteredCaptcha" 
                         name="enteredCaptcha" 
                         maxLength="4" 
                         placeholder="Enter 4-Digit Captcha"
                         style="letter-spacing: 0.5rem; text-transform: uppercase;"
                         (keydown.enter)="$event.preventDefault(); confirmReset()">
                  <div *ngIf="captchaError" class="text-danger small mt-2 fw-semibold">
                    <i class="bi bi-x-circle-fill"></i> {{ captchaError }}
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer bg-light border-0">
              <button type="button" class="btn btn-secondary px-4 py-2" style="border-radius: 6px;" (click)="showResetModal = false">Cancel</button>
              <button type="button" class="btn text-white px-4 py-2" style="background-color: #8b2635; border-radius: 6px;" 
                      [disabled]="!enteredCaptcha || isResetting" (click)="confirmReset()">
                <span *ngIf="isResetting" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                {{ isResetting ? 'Resetting...' : 'Permanently Reset' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      
    </div>
  `
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  isImporting = false;       // kept for legacy (old /upload endpoint)
  isPreviewLoading = false;  // scanning phase
  isApplying = false;        // apply phase
  showModal = false;
  isEditMode = false;

  currentEmployee: Employee = this.getEmptyEmployee();

  // Preview modal state
  showPreviewModal = false;
  previewResult: ImportPreviewResult | null = null;
  pendingFile: File | null = null;
  selectedNewMemberIds = new Set<string>();
  selectedUpdateMemberIds = new Set<string>();
  addAsNewMemberIds = new Set<string>();

  // Summary modal state
  showImportSummaryModal = false;
  importSummary: EmployeeImportSummary | null = null;

  showResetModal = false;
  captchaText = '';
  enteredCaptcha = '';
  captchaError = '';
  isResetting = false;

  private lastCanvas: HTMLCanvasElement | null = null;

  @ViewChild('captchaCanvas') set captchaCanvas(content: ElementRef<HTMLCanvasElement>) {
    if (content) {
      if (content.nativeElement !== this.lastCanvas) {
        this.lastCanvas = content.nativeElement;
        setTimeout(() => {
          this.drawCaptcha(content.nativeElement);
        }, 100);
      }
    } else {
      this.lastCanvas = null;
    }
  }

  constructor(private http: HttpClient, private dialogService: DialogService) { }

  ngOnInit() {
    this.loadEmployees();
  }

  loadEmployees() {
    this.http.get<Employee[]>(`${environment.apiUrl}/employees`).subscribe({
      next: (data) => this.employees = data,
      error: (err) => console.error('Failed to load employees', err)
    });
  }

  getEmptyEmployee(): Employee {
    return {
      memberId: '', fullName: '', uanNumber: '', ipNumber: '',
      bankAccountNo: '', ifscCode: '', status: 'ACTIVE', category: 'CL'
    };
  }

  openAddModal() {
    this.isEditMode = false;
    this.currentEmployee = this.getEmptyEmployee();
    this.showModal = true;
  }

  openEditModal(emp: Employee) {
    this.isEditMode = true;
    this.currentEmployee = { ...emp }; // Clone
    this.showModal = true;
  }

  saveEmployee() {
    if (this.isEditMode) {
      this.http.put<Employee>(`${environment.apiUrl}/employees/${this.currentEmployee.id}`, this.currentEmployee)
        .subscribe({
          next: () => {
            this.loadEmployees();
            this.showModal = false;
            this.dialogService.alert('Success', 'Employee updated successfully!');
          },
          error: (err) => {
            console.error(err);
            this.dialogService.alert('Error', 'Failed to update: ' + (err.error?.message || err.message));
          }
        });
    } else {
      this.http.post<Employee>(`${environment.apiUrl}/employees`, this.currentEmployee)
        .subscribe({
          next: () => {
            this.loadEmployees();
            this.showModal = false;
            this.dialogService.alert('Success', 'Employee added successfully!');
          },
          error: (err) => {
            console.error(err);
            this.dialogService.alert('Error', 'Failed to add: ' + (err.error?.message || err.message));
          }
        });
    }
  }

  deleteEmployee(id: string) {
    this.dialogService.confirm('Delete Employee', 'Are you sure you want to delete this employee? This action cannot be undone.')
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.http.delete(`${environment.apiUrl}/employees/${id}`).subscribe({
          next: () => {
            this.loadEmployees();
            this.dialogService.alert('Success', 'Employee deleted successfully.');
          },
          error: (err) => {
            console.error(err);
            this.dialogService.alert('Error', 'Failed to delete: ' + (err.error?.message || err.message));
          }
        });
      });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;
    event.target.value = '';

    this.pendingFile = file;
    this.isPreviewLoading = true;
    this.employees = [];

    const formData = new FormData();
    formData.append('file', file);

    this.http.post<ImportPreviewResult>(`${environment.apiUrl}/employees/preview`, formData).subscribe({
      next: (result) => {
        this.previewResult = result;
        // Auto-select all new and all updates by default
        this.selectedNewMemberIds = new Set(result.toCreate.map(e => e.memberId));
        this.selectedUpdateMemberIds = new Set(result.toUpdate.map(e => e.memberId));
        this.isPreviewLoading = false;
        this.loadEmployees();  // Reload list while modal is opening
        this.showPreviewModal = true;
      },
      error: (err) => {
        this.isPreviewLoading = false;
        this.loadEmployees();
        console.error(err);
        this.dialogService.alert('Error', 'Scan failed: ' + (err.error?.message || err.message));
      }
    });
  }

  cancelPreview() {
    this.showPreviewModal = false;
    this.previewResult = null;
    this.pendingFile = null;
    this.selectedNewMemberIds = new Set();
    this.selectedUpdateMemberIds = new Set();
    this.addAsNewMemberIds = new Set();
  }

  toggleNew(memberId: string, checked: boolean) {
    if (checked) this.selectedNewMemberIds.add(memberId);
    else this.selectedNewMemberIds.delete(memberId);
    this.selectedNewMemberIds = new Set(this.selectedNewMemberIds); // trigger change detection
  }

  toggleUpdate(memberId: string, checked: boolean) {
    if (checked) this.selectedUpdateMemberIds.add(memberId);
    else this.selectedUpdateMemberIds.delete(memberId);
    this.selectedUpdateMemberIds = new Set(this.selectedUpdateMemberIds);
  }

  onChangeAction(memberId: string, action: string) {
    if (action === 'NEW') {
      this.addAsNewMemberIds.add(memberId);
    } else {
      this.addAsNewMemberIds.delete(memberId);
    }
    this.addAsNewMemberIds = new Set(this.addAsNewMemberIds);
  }

  selectAllNew(checked: boolean) {
    this.selectedNewMemberIds = checked
      ? new Set(this.previewResult?.toCreate.map(e => e.memberId) ?? [])
      : new Set();
  }

  selectAllUpdates(checked: boolean) {
    this.selectedUpdateMemberIds = checked
      ? new Set(this.previewResult?.toUpdate.map(e => e.memberId) ?? [])
      : new Set();
  }

  confirmAndApply() {
    if (!this.pendingFile) return;
    const approvedIds = [
      ...this.selectedNewMemberIds,
      ...this.selectedUpdateMemberIds
    ].join(',');
    const addAsNewIds = [...this.addAsNewMemberIds].join(',');

    const formData = new FormData();
    formData.append('file', this.pendingFile);
    formData.append('approvedMemberIds', approvedIds);
    formData.append('addAsNewMemberIds', addAsNewIds);

    this.isApplying = true;
    this.http.post<EmployeeImportSummary>(`${environment.apiUrl}/employees/apply`, formData).subscribe({
      next: (summary) => {
        this.http.get<Employee[]>(`${environment.apiUrl}/employees`).subscribe({
          next: (data) => {
            this.employees = data;
            this.isApplying = false;
            this.showPreviewModal = false;
            this.importSummary = summary;
            this.showImportSummaryModal = true;
            this.pendingFile = null;
          },
          error: () => {
            this.isApplying = false;
            this.showPreviewModal = false;
            this.importSummary = summary;
            this.showImportSummaryModal = true;
          }
        });
      },
      error: (err) => {
        this.isApplying = false;
        console.error(err);
        this.dialogService.alert('Error', 'Apply failed: ' + (err.error?.message || err.message));
      }
    });
  }

  onlyNumbers(event: any) {
    const pattern = /[0-9]/;
    const inputChar = String.fromCharCode(event.charCode);
    if (!pattern.test(inputChar)) {
      event.preventDefault();
    }
  }

  openResetModal() {
    this.enteredCaptcha = '';
    this.captchaError = '';
    this.isResetting = false;
    this.generateCaptchaText();
    this.showResetModal = true;
  }

  generateCaptchaText() {
    this.captchaText = Math.floor(1000 + Math.random() * 9000).toString();
  }

  generateCaptcha() {
    this.enteredCaptcha = '';
    this.captchaError = '';
    this.generateCaptchaText();
    setTimeout(() => {
      if (this.lastCanvas) {
        this.drawCaptcha(this.lastCanvas);
      }
    }, 100);
  }

  drawCaptcha(canvas: HTMLCanvasElement) {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
    gradient.addColorStop(0, '#f8f9fa');
    gradient.addColorStop(1, '#e9ecef');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    for (let i = 0; i < 5; i++) {
      ctx.strokeStyle = `rgba(${Math.floor(Math.random() * 150)}, ${Math.floor(Math.random() * 150)}, ${Math.floor(Math.random() * 150)}, 0.3)`;
      ctx.lineWidth = 1.5 + Math.random() * 1.5;
      ctx.beginPath();
      ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
      ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
      ctx.stroke();
    }

    for (let i = 0; i < 30; i++) {
      ctx.fillStyle = `rgba(${Math.floor(Math.random() * 200)}, ${Math.floor(Math.random() * 200)}, ${Math.floor(Math.random() * 200)}, 0.4)`;
      ctx.beginPath();
      ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, 1 + Math.random() * 2, 0, Math.PI * 2);
      ctx.fill();
    }

    const colors = ['#2c3e50', '#8b2635', '#2c5f2d', '#1e4d2b', '#111111'];
    ctx.font = 'bold 30px "Courier New", Courier, monospace';
    ctx.textBaseline = 'middle';

    const charWidth = canvas.width / 5;
    for (let i = 0; i < this.captchaText.length; i++) {
      const char = this.captchaText[i];
      const x = charWidth * (i + 0.8) + (Math.random() * 5 - 2.5);
      const y = canvas.height / 2 + (Math.random() * 8 - 4);

      ctx.fillStyle = colors[Math.floor(Math.random() * colors.length)];
      const angle = (Math.random() * 30 - 15) * Math.PI / 180;
      
      ctx.save();
      ctx.translate(x, y);
      ctx.rotate(angle);
      ctx.fillText(char, 0, 0);
      ctx.restore();
    }
  }

  confirmReset() {
    if (this.enteredCaptcha.trim() !== this.captchaText.trim()) {
      this.captchaError = 'Incorrect Captcha. Please try again.';
      this.generateCaptcha();
      return;
    }

    this.isResetting = true;
    this.captchaError = '';

    this.http.post<any>(`${environment.apiUrl}/employees/reset-db`, {}).subscribe({
      next: (response) => {
        this.isResetting = false;
        if (response.status === 'success') {
          this.showResetModal = false;
          this.loadEmployees();
          this.dialogService.alert('Database Reset', 'The database has been successfully reset to its default seed state.');
        } else {
          this.captchaError = 'Failed to reset: ' + response.message;
          this.generateCaptcha();
        }
      },
      error: (err) => {
        this.isResetting = false;
        console.error(err);
        this.captchaError = 'Reset failed: ' + (err.error?.message || err.message);
        this.generateCaptcha();
      }
    });
  }
}
