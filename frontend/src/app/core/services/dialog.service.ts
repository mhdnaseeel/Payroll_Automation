import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { take } from 'rxjs/operators';

export interface DialogOptions {
    title: string;
    message: string;
    type: 'ALERT' | 'CONFIRM';
    confirmText?: string;
    cancelText?: string;
}

@Injectable({
    providedIn: 'root'
})
export class DialogService {
    private dialogSubject = new Subject<DialogOptions>();
    private confirmationSubject = new Subject<boolean>();

    dialogState$ = this.dialogSubject.asObservable();

    alert(title: string, message: string): Observable<boolean> {
        this.dialogSubject.next({
            title,
            message,
            type: 'ALERT',
            confirmText: 'OK'
        });
        return this.confirmationSubject.asObservable().pipe(take(1));
    }

    confirm(title: string, message: string): Observable<boolean> {
        this.dialogSubject.next({
            title,
            message,
            type: 'CONFIRM',
            confirmText: 'Yes, Proceed',
            cancelText: 'Cancel'
        });
        return this.confirmationSubject.asObservable().pipe(take(1));
    }

    resolve(result: boolean) {
        this.confirmationSubject.next(result);
    }
}
