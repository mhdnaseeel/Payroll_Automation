
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
            <button class="btn border-0 shadow-sm text-white" style="background-color: #2c5f2d; font-weight: 500;" (click)="fileInput.click()">
                <i class="bi bi-upload"></i> Import Excel
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
  showModal = false;
  isEditMode = false;

  currentEmployee: Employee = this.getEmptyEmployee();

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
    if (file) {
      const formData = new FormData();
      formData.append('file', file);

      this.http.post<Employee[]>(`${environment.apiUrl}/employees/upload`, formData).subscribe({
        next: (newEmps) => {
          this.employees = [...this.employees, ...newEmps]; // Append new
          this.dialogService.alert('Success', `Successfully imported ${newEmps.length} employees!`);
          event.target.value = ''; // Reset input to allow re-selection
        },
        error: (err) => {
          console.error(err);
          this.dialogService.alert('Error', 'Upload failed: ' + (err.error?.message || err.message));
          event.target.value = ''; // Reset input to allow re-selection
        }
      });
    }
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
