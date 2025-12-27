
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap, Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';


export interface PayrollPeriod {
    id: string;
    month: number;
    year: number;
    status: 'OPEN' | 'CLOSED';
    lastWorkingDay?: string; // Optional because backend v1 didn't have it, v2 does.
}

@Injectable({
    providedIn: 'root'
})
export class PayrollService {
    private apiUrl = `${environment.apiUrl}/payroll`;

    // Track current active period ID and Status
    private currentPeriodSubject = new BehaviorSubject<PayrollPeriod | null>(null);
    public currentPeriod$ = this.currentPeriodSubject.asObservable();

    // GLOBAL SELECTION STATE (For Dashboard -> Modules linking)
    private globalSelectionSubject = new BehaviorSubject<{ month: number, year: number } | null>(null);
    public globalSelection$ = this.globalSelectionSubject.asObservable();

    setGlobalSelection(month: number, year: number) {
        this.globalSelectionSubject.next({ month, year });
    }

    getGlobalSelection() {
        return this.globalSelectionSubject.value;
    }

    constructor(private http: HttpClient) { }

    loadPeriod(periodId: string) {
        // "latest" is a special keyword our backend mock handles, 
        // strictly speaking we should fetch actual ID first.
        // For Prototype, let's assume we fetch the Single active period.
        this.http.get<PayrollPeriod>(`${this.apiUrl}/periods/${periodId}`).subscribe(period => {
            this.currentPeriodSubject.next(period);
        });
    }

    finalizePeriod(id: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/periods/${id}/close`, {});
    }

    closePeriod(periodId: string) {
        return this.http.post<PayrollPeriod>(`${this.apiUrl}/periods/${periodId}/close`, {}).pipe(
            tap(updated => {
                this.currentPeriodSubject.next(updated);
                this.refreshPeriods();
            })
        );
    }

    reopenPeriod(periodId: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/periods/${periodId}/reopen`, {}).pipe(
            tap(updated => {
                this.currentPeriodSubject.next(updated);
                this.refreshPeriods();
            })
        );
    }

    createPeriod(payload: any): Observable<PayrollPeriod> {
        return this.http.post<PayrollPeriod>(`${this.apiUrl}/periods`, payload).pipe(
            tap(() => this.refreshPeriods())
        );
    }

    // Cache for periods to avoid repeated API calls
    private periodsCache$: Observable<PayrollPeriod[]> | null = null;

    getPeriods() {
        if (!this.periodsCache$) {
            this.periodsCache$ = this.http.get<PayrollPeriod[]>(`${this.apiUrl}/periods`).pipe(
                // Cache the last emitted value
                // In a real app we might want a timer or a manual refresh trigger
                // For now, this fixes the "re-fetch on every nav" issue
                tap(periods => console.log('Fetched periods from API', periods.length)),
                // shareReplay(1) // Replay last 1 emission to new subscribers
                // Actually, simple sharing might be enough if we just want to dedup active subscribers
                // But shareReplay is better for caching across navigation
                shareReplay({ bufferSize: 1, refCount: true })
            );
        }
        return this.periodsCache$;
    }

    // Call this when adding/closing regular periods force refresh
    refreshPeriods() {
        this.periodsCache$ = null;
    }

    downloadTemplate() {
        return this.http.get(`${this.apiUrl}/import/template`, { responseType: 'blob', observe: 'response' });
    }

    importEntries(file: File, periodId: string): Observable<any> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<any>(`${this.apiUrl}/import/${periodId}`, formData);
    }

    importUtr(file: File, periodId: string): Observable<any> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<any>(`${this.apiUrl}/import/utr/${periodId}`, formData);
    }
}
