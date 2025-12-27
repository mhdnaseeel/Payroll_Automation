import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

import { DialogService } from './services/dialog.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
    constructor(private authService: AuthService, private dialogService: DialogService) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(request).pipe(
            catchError((error: HttpErrorResponse) => {
                // Status 0: Connection Refused (Backend down)
                // Status 401: Unauthorized (Token invalid)
                if (error.status === 0 || error.status === 401) {
                    if (this.authService.isLoggingOut) {
                        return throwError(() => error);
                    }

                    console.error('Session Invalid or Server Down. Logging out...');
                    this.authService.logout();

                    // Optional: Redirect to login or show alert
                    if (error.status === 0) {
                        this.dialogService.alert('Connection Error', 'Connection lost. Please check if the server is running.');
                    } else {
                        this.dialogService.alert('Session Expired', 'Session expired. Please login again.');
                    }

                    window.location.href = '/login';
                }
                return throwError(() => error);
            })
        );
    }
}
