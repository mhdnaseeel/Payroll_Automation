import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DialogService } from './services/dialog.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
    constructor(private dialogService: DialogService) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(request).pipe(
            catchError((error: HttpErrorResponse) => {
                // TokenInterceptor handles 401. We focus on other errors here.
                if (error.status === 0) {
                    console.error('Connection Refused (Backend down)');
                    this.dialogService.alert('Connection Error', 'Connection lost. Please check if the server is running.');
                }
                // Optional: Global 500 handler?
                return throwError(() => error);
            })
        );
    }
}
