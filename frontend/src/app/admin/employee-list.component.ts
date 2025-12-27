
import { Component, OnInit } from '@angular/core';
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
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2>Employee Master (Admin)</h2>
        <div class="d-flex gap-2">
            <input type="file" #fileInput (change)="onFileSelected($event)" style="display:none" accept=".xlsx, .xls">
            <button class="btn border-0 shadow-sm text-white" style="background-color: #2c5f2d; font-weight: 500;" (click)="fileInput.click()">
                <i class="bi bi-upload"></i> Import Excel
            </button>
            <button class="btn border-0 shadow-sm text-white" style="background-color: #0f2615;" (click)="openAddModal()">+ Add New</button>
        </div>
      </div>

      <div class="card shadow">
        <div class="card-body p-0">
          <table class="table table-striped mb-0">
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
      
    </div>
  `
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  showModal = false;
  isEditMode = false;

  currentEmployee: Employee = this.getEmptyEmployee();

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
}
