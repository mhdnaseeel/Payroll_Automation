package com.fci.automation.controller;

import com.fci.automation.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200", exposedHeaders = "Content-Disposition")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private com.fci.automation.repository.PayrollPeriodRepository periodRepository;

    // 1. PDF Reports
    @GetMapping("/{periodId}/main-file")
    public ResponseEntity<byte[]> getMainFilePdf(@PathVariable UUID periodId) {
        byte[] pdf = reportService.generatePdfReport(periodId, "Main File (Payroll Engine)");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=main_file.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{periodId}/payment-details")
    public ResponseEntity<byte[]> getPaymentDetailsPdf(@PathVariable UUID periodId) {
        byte[] pdf = reportService.generatePaymentDetailsPdf(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payment_details.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{periodId}/wage-summary")
    public ResponseEntity<byte[]> getWageSummaryPdf(@PathVariable UUID periodId) {
        byte[] pdf = reportService.generateWageSummaryPdf(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=wage_summary.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{periodId}/attendance")
    public ResponseEntity<byte[]> getAttendanceRegisterPdf(@PathVariable UUID periodId) {
        byte[] pdf = reportService.generateAttendanceRegisterPdf(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_register.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // 2. Excel (ESI)
    @GetMapping("/{periodId}/esi")
    public ResponseEntity<byte[]> getEsiExcel(@PathVariable UUID periodId) {
        // Fetch period to construct filename
        com.fci.automation.entity.PayrollPeriod period = periodRepository.findById(periodId).orElseThrow();
        String monthName = java.time.Month.of(period.getMonth()).name().toLowerCase().substring(0, 3);
        String filename = String.format("%s_%d_esi.xls", monthName, period.getYear());

        byte[] excel = reportService.generateEsiExcel(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    // 3. CSV (EPF)
    // 3. Text (EPF)
    @GetMapping("/{periodId}/epf")
    public ResponseEntity<String> getEpfTxt(@PathVariable UUID periodId) {
        // Fetch period to construct filename
        com.fci.automation.entity.PayrollPeriod period = periodRepository.findById(periodId).orElseThrow();
        String monthName = java.time.Month.of(period.getMonth()).name().toLowerCase().substring(0, 3); // "nov"
        String filename = String.format("%s_%d_epf.txt", monthName, period.getYear());

        String txt = reportService.generateEpfTxt(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(txt);
    }

    @GetMapping("/{periodId}/bulk")
    public ResponseEntity<String> getBulkTxt(
            @PathVariable UUID periodId,
            @RequestParam(value = "paymentDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate paymentDate) {

        // Default to today if not provided
        if (paymentDate == null) {
            paymentDate = java.time.LocalDate.now();
        }

        com.fci.automation.entity.PayrollPeriod period = periodRepository.findById(periodId).orElseThrow();
        String monthName = java.time.Month.of(period.getMonth()).name().toLowerCase().substring(0, 3);
        String filename = String.format("%s_bulk_payment.txt", monthName);

        String txt = reportService.generateBulkTxt(periodId, paymentDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(txt);
    }
}
