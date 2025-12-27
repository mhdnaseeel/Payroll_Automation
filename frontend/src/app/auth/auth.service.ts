import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';

import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/auth`;
    private userRoleSubject = new BehaviorSubject<string | null>(sessionStorage.getItem('user_role'));
    public userRole$ = this.userRoleSubject.asObservable();
    public isLoggingOut = false;

    constructor(private http: HttpClient, private router: Router) { }

    login(username: string, password: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/login`, { username, password }).pipe(
            tap(response => {
                if (response.token) {
                    sessionStorage.setItem('auth_token', response.token);
                    sessionStorage.setItem('refresh_token', response.refreshToken);
                    sessionStorage.setItem('user_role', response.role);

                    this.userRoleSubject.next(response.role);
                    this.isLoggingOut = false;
                }
            })
        );
    }

    refreshToken(): Observable<any> {
        const refreshToken = sessionStorage.getItem('refresh_token');
        return this.http.post<any>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
            tap(response => {
                sessionStorage.setItem('auth_token', response.accessToken);
                // Refresh token might be rotated, update if provided
                if (response.refreshToken) {
                    sessionStorage.setItem('refresh_token', response.refreshToken);
                }
            }),
            catchError(err => {
                this.logout();
                return throwError(() => err);
            })
        );
    }

    logout() {
        this.isLoggingOut = true;
        sessionStorage.clear();
        this.userRoleSubject.next(null);
        this.router.navigate(['/login']);
    }

    getRole(): string | null {
        return this.userRoleSubject.value;
    }

    getToken(): string | null {
        return sessionStorage.getItem('auth_token');
    }
}
