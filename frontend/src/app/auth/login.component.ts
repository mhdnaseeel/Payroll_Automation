
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="d-flex justify-content-center align-items-center vh-100" style="background-color: #f2f0eb;">
      <div class="col-md-4">
        <div class="card shadow-lg border-0 rounded-4" style="background-color: #ffffff;">
          <div class="card-header text-center py-4 rounded-top-4" style="background-color: #0f2615; color: #e8e6e1; border-bottom: 4px solid #1e4d2b;">
            <h3 class="mb-0 fw-bold" style="letter-spacing: 1px;">FCI Automation</h3>
            <small class="text-uppercase" style="letter-spacing: 2px; font-size: 0.7rem; color: #a3b18a;">Premium Secure Access</small>
          </div>
          <div class="card-body p-5">
            <div class="mb-4">
              <label class="form-label fw-bold text-dark" style="font-size: 0.9rem; letter-spacing: 0.5px;">USERNAME</label>
              <div class="input-group">
                <span class="input-group-text border-end-0 text-white" style="background-color: #2c5f2d;"><i class="bi bi-person"></i></span>
                <input type="text" class="form-control border-start-0 bg-light" placeholder="Enter username" [(ngModel)]="username">
              </div>
            </div>
            <div class="mb-4">
              <label class="form-label fw-bold text-dark" style="font-size: 0.9rem; letter-spacing: 0.5px;">PASSWORD</label>
              <div class="input-group">
                <span class="input-group-text border-end-0 text-white" style="background-color: #2c5f2d;"><i class="bi bi-lock"></i></span>
                <input type="password" class="form-control border-start-0 bg-light" placeholder="Enter password" [(ngModel)]="password">
              </div>
            </div>
            <button class="btn w-100 py-2 fw-bold text-uppercase shadow-sm text-white" 
                    style="background-color: #1e4d2b; letter-spacing: 2px; transition: 0.3s;"
                    onmouseover="this.style.backgroundColor='#14361f'"
                    onmouseout="this.style.backgroundColor='#1e4d2b'"
                    (click)="onLogin()">
              Login
            </button>
            <p class="text-danger mt-3 text-center small fw-bold" *ngIf="errorMessage">
              <i class="bi bi-exclamation-circle me-1"></i> {{ errorMessage }}
            </p>
          </div>
          <div class="card-footer text-center py-3 border-0 rounded-bottom-4" style="background-color: #f2f0eb;">
            <small style="color: #555;">FCI Payroll System &copy; 2025</small>
          </div>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit() {
    const role = this.authService.getRole();
    if (role === 'ADMIN') {
      this.router.navigate(['/admin/employees'], { replaceUrl: true });
    } else if (role === 'USER') {
      this.router.navigate(['/user/home'], { replaceUrl: true });
    } else if (role === 'BILL') {
      this.router.navigate(['/billing'], { replaceUrl: true });
    }
  }

  onLogin() {
    this.authService.login(this.username, this.password).subscribe({
      next: (res) => {
        if (res.role === 'ADMIN') {
          this.router.navigate(['/admin/employees']);
        } else if (res.role === 'BILL') {
          this.router.navigate(['/billing']);
        } else {
          // Find active period logic would be better, for now redirect to dashboard or payroll
          this.router.navigate(['/user/home']);
        }
      },
      error: (err) => {
        console.error('Login error:', err);
        this.errorMessage = `Login Failed: ${err.status} - ${err.statusText || 'Unknown Error'}`;
        if (err.status === 0) {
          this.errorMessage = 'Connection Failed. Is Backend running? (CORS/Network)';
        } else if (err.status === 401 || err.status === 500) { // 500 can happen if our manual RuntimeException is treated as server error
          this.errorMessage = 'Invalid Credentials';
        }
      }
    });
  }
}
