import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DailyBagsWork {
    id?: string;
    date: string; // YYYY-MM-DD
    workSlipNo?: string;
    bagsUpto10: number;
    bags11to16: number;
    bags17to20: number;
    bagsAbove20: number;
}

export interface DailyClauseWork {
    id?: string;
    date: string; // YYYY-MM-DD
    workSlipNo?: string;
    bagsClause15: number;
}

export interface DailyHeadcount {
    id?: string;
    date: string; // YYYY-MM-DD
    headcount: number;
}

export interface SalaryCalculationResult {
    date: string;
    totalAmount: number;
    totalAttendance: number;
    perPersonSalary: number;
}

@Injectable({
    providedIn: 'root'
})
export class SalaryService {
    private apiUrl = `${environment.apiUrl}/salary`;

    constructor(private http: HttpClient) { }

    saveBagsWork(work: DailyBagsWork): Observable<DailyBagsWork> {
        return this.http.post<DailyBagsWork>(`${this.apiUrl}/bags`, work);
    }

    saveClauseWork(work: DailyClauseWork): Observable<DailyClauseWork> {
        return this.http.post<DailyClauseWork>(`${this.apiUrl}/clause`, work);
    }

    saveHeadcount(headcount: DailyHeadcount): Observable<DailyHeadcount> {
        return this.http.post<DailyHeadcount>(`${this.apiUrl}/headcount`, headcount);
    }

    getDailyCalculation(date: string): Observable<SalaryCalculationResult> {
        return this.http.get<SalaryCalculationResult>(`${this.apiUrl}/daily/${date}`);
    }

    getCalculationsRange(startDate: string, endDate: string): Observable<SalaryCalculationResult[]> {
        return this.http.get<SalaryCalculationResult[]>(`${this.apiUrl}/range?start=${startDate}&end=${endDate}`);
    }
}
