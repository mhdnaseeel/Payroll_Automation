import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogService, DialogOptions } from '../services/dialog.service';

@Component({
    selector: 'app-confirm-dialog',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="modal d-block" *ngIf="visible" tabindex="-1" style="background: rgba(0,0,0,0.5); z-index: 2000;">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow-lg border-0">
          <div class="modal-header text-white" style="background-color: #2c3e50;">
            <h5 class="modal-title fw-bold">
               <i class="bi" [ngClass]="options.type === 'ALERT' ? 'bi-info-circle-fill' : 'bi-question-circle-fill'"></i>
               {{ options.title }}
            </h5>
          </div>
          <div class="modal-body p-4">
            <p class="mb-0 fs-5 text-secondary">{{ options.message }}</p>
          </div>
          <div class="modal-footer bg-light">
            <button *ngIf="options.type === 'CONFIRM'" 
                    type="button" 
                    class="btn btn-outline-secondary px-4 fw-medium" 
                    (click)="resolve(false)">
              {{ options.cancelText || 'Cancel' }}
            </button>
            <button type="button" 
                    class="btn btn-primary px-4 fw-bold" 
                    style="background-color: #2c3e50; border-color: #2c3e50;"
                    (click)="resolve(true)">
              {{ options.confirmText || 'OK' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ConfirmDialogComponent implements OnInit {
    visible = false;
    options: DialogOptions = { title: '', message: '', type: 'ALERT' };

    constructor(private dialogService: DialogService) { }

    ngOnInit() {
        this.dialogService.dialogState$.subscribe(opts => {
            this.options = opts;
            this.visible = true;
        });
    }

    resolve(result: boolean) {
        this.visible = false;
        this.dialogService.resolve(result);
    }
}
