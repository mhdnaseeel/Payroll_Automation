import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class MonthSelectionService {
    private currentYear = new Date().getFullYear();
    private currentMonth = new Date().getMonth() + 1;

    private selectionSubject = new BehaviorSubject<{ month: number, year: number }>({
        month: this.currentMonth,
        year: this.currentYear
    });

    selection$ = this.selectionSubject.asObservable();

    constructor() { }

    setSelection(month: number, year: number) {
        this.selectionSubject.next({ month, year });
    }

    getSelection() {
        return this.selectionSubject.value;
    }
}
