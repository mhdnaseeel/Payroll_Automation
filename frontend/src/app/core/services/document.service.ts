import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';


export interface UploadDocument {
    id: string;
    type: string;
    subType: string;
    fileName: string;
    filePath: string;
    uploadDate: string;
}

@Injectable({
    providedIn: 'root'
})
export class DocumentService {
    private apiUrl = `${environment.apiUrl}/upload`;

    constructor(private http: HttpClient) { }

    uploadFile(file: File, type: string, subType: string, periodId: string): Observable<UploadDocument> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', type);
        formData.append('subType', subType);
        formData.append('periodId', periodId);

        return this.http.post<UploadDocument>(this.apiUrl, formData);
    }

    getDocuments(periodId?: string): Observable<UploadDocument[]> {
        let url = this.apiUrl;
        if (periodId) {
            url += `?periodId=${periodId}`;
        }
        return this.http.get<UploadDocument[]>(url);
    }

    downloadFile(id: string): void {
        const url = `${this.apiUrl}/${id}/download`;
        this.http.get(url, { responseType: 'blob', observe: 'response' }).subscribe({
            next: (response) => {
                const blob = response.body;
                if (!blob) {
                    console.error('Download failed: Empty body');
                    return;
                }
                const contentDisposition = response.headers.get('Content-Disposition');
                let filename = 'download.pdf';
                if (contentDisposition) {
                    const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
                    if (matches != null && matches[1]) {
                        filename = matches[1].replace(/['"]/g, '');
                    }
                }

                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.download = filename;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                window.URL.revokeObjectURL(downloadUrl);
            },
            error: (err) => console.error('Download failed', err)
        });
    }
}
