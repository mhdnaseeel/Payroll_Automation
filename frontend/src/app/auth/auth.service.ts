
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { DialogService } from '../core/services/dialog.service';
import { environment } from '../../environments/environment';


@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/auth`;
    // "ADMIN" | "USER" | null
    private userRoleSubject = new BehaviorSubject<string | null>(sessionStorage.getItem('user_role'));
    public userRole$ = this.userRoleSubject.asObservable();

    constructor(private http: HttpClient, private dialogService: DialogService) { }

    login(username: string, password: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/login`, { username, password }).pipe(
            tap(response => {
                if (response.token) {
                    // Update: Store the FULL token (mock-token:UUID) as the auth_token.
                    // This ensures every request sends the session ID.
                    const token = response.token;

                    // We can still parse it if we need the session ID explicitly, but
                    // storing the full string is key for the Interceptor.
                    const parts = token.split(':');
                    const serverSessionId = parts.length > 1 ? parts[1] : '';

                    sessionStorage.setItem('auth_token', token);
                    sessionStorage.setItem('server_session_id', serverSessionId);

                    this.setRole(response.role);
                    sessionStorage.setItem('user_role', response.role);
                    sessionStorage.setItem('original_role', response.role); // Track original login role
                }
            })
        );
    }

    checkServerSession() {
        this.http.get<any>(`${this.apiUrl}/status`).subscribe({
            next: (status) => {
                const storedSessionId = sessionStorage.getItem('server_session_id');
                if (storedSessionId && status.sessionId !== storedSessionId) {
                    console.warn('Backend restarted. Session invalid.');
                    this.logout();
                    this.dialogService.alert('Server Restarted', 'Server was restarted. Please login again.');
                    window.location.reload();
                }
            },
            error: () => {
                // Server down?
                // ErrorInterceptor handles 0 status, but just in case
            }
        });
    }

    getOriginalRole(): string | null {
        return sessionStorage.getItem('original_role');
    }

    switchRole(targetRole: string) {
        this.setRole(targetRole);
        sessionStorage.setItem('user_role', targetRole);
        // Force refresh or let the subscription handle it? 
        // Subscription handles UI, but we might want to redirect.
    }

    public isLoggingOut = false;

    logout() {
        this.isLoggingOut = true;
        sessionStorage.removeItem('auth_token');
        sessionStorage.removeItem('user_role');
        sessionStorage.removeItem('original_role');
        sessionStorage.removeItem('server_session_id');
        this.userRoleSubject.next(null);
    }

    getRole(): string | null {
        return this.userRoleSubject.value;
    }

    private setRole(role: string) {
        this.userRoleSubject.next(role);
    }
}
