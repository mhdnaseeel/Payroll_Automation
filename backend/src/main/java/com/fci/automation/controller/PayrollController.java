package com.fci.automation.controller;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import com.fci.automation.service.PayrollCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "http://localhost:4200")
public class PayrollController {

    @Autowired
    private PayrollPeriodRepository periodRepository;

    @Autowired
    private PayrollEntryRepository entryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayrollCalculatorService calculatorService;

    // 1. Start/Open Month (Find or Create)
    @PostMapping("/periods")
    public org.springframework.http.ResponseEntity<?> createPeriod(
            @RequestBody com.fci.automation.dto.PayrollPeriodRequest request) {
        try {
            // Validation
            if (request.getMonth() == null || request.getYear() == null) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "Month and Year are required."));
            }

            // Manual Date Parsing Logic
            java.time.LocalDate lwd = null;
            String dateStr = request.getLastWorkingDay();

            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    // Try ISO first (YYYY-MM-DD)
                    lwd = java.time.LocalDate.parse(dateStr);
                } catch (java.time.format.DateTimeParseException e1) {
                    try {
                        // Try DD/MM/YYYY
                        lwd = java.time.LocalDate.parse(dateStr,
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (java.time.format.DateTimeParseException e2) {
                        // Try MM/DD/YYYY (US fallback)
                        try {
                            lwd = java.time.LocalDate.parse(dateStr,
                                    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        } catch (Exception e3) {
                            return org.springframework.http.ResponseEntity.badRequest()
                                    .body(java.util.Map.of("message",
                                            "Invalid Date Format. Please use YYYY-MM-DD or DD/MM/YYYY."));
                        }
                    }
                }
            }

            PayrollPeriod period = new PayrollPeriod();
            period.setMonth(request.getMonth());
            period.setYear(request.getYear());
            period.setLastWorkingDay(lwd);

            // Check if period already exists for this MM/YYYY
            java.util.Optional<PayrollPeriod> existing = periodRepository.findByMonthAndYear(period.getMonth(),
                    period.getYear());
            if (existing.isPresent()) {
                PayrollPeriod existingPeriod = existing.get();
                // Update Last Working Day if provided, to ensure consistency
                if (lwd != null) {
                    existingPeriod.setLastWorkingDay(lwd);
                    periodRepository.save(existingPeriod);
                }
                return org.springframework.http.ResponseEntity.ok(existingPeriod);
            }

            // Start new period
            period.setStatus(PayrollPeriod.Status.OPEN);
            return org.springframework.http.ResponseEntity.ok(periodRepository.save(period));

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message", "Payroll period already exists for this month/year."));
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError()
                    .body(java.util.Map.of("message", "Error creating period: " + e.getMessage()));
        }
    }

    // 1.1 List All Periods (For History/Reports)
    @GetMapping("/periods")
    public List<PayrollPeriod> getAllPeriods() {
        return periodRepository.findAllByOrderByYearDescMonthDesc();
    }

    // 2. Load Entry Sheet (Grid)
    @GetMapping("/periods/{periodIdStr}/entries")
    public List<PayrollEntry> getEntries(@PathVariable String periodIdStr) {
        UUID periodId;
        if ("latest".equalsIgnoreCase(periodIdStr)) {
            // Find latest by ordering (Year DESC, Month DESC)
            PayrollPeriod latest = periodRepository.findAllByOrderByYearDescMonthDesc().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        // If no periods exist, create a default one (e.g. current month)
                        // Using dynamic current date instead of hardcoded
                        java.time.LocalDate now = java.time.LocalDate.now();
                        PayrollPeriod p = new PayrollPeriod();
                        p.setMonth(now.getMonthValue());
                        p.setYear(now.getYear());
                        p.setStatus(PayrollPeriod.Status.OPEN);
                        return periodRepository.save(p);
                    });
            periodId = latest.getId();
        } else {
            periodId = UUID.fromString(periodIdStr);
        }

        List<PayrollEntry> entries = entryRepository.findByPeriodId(periodId);

        // If empty, creating entries for all active employees (Auto-population)
        if (entries.isEmpty()) {
            PayrollPeriod period = periodRepository.findById(periodId).orElseThrow();
            List<Employee> employees = employeeRepository.findAll();

            // Filter out employees who left BEFORE this period started
            // i.e., inactiveDate < period.startDate (First of Month)
            java.time.LocalDate periodStart = java.time.LocalDate.of(period.getYear(), period.getMonth(), 1);

            entries = employees.stream()
                    .filter(emp -> {
                        if (emp.getInactiveDate() == null)
                            return true; // Always include if no date set
                        // Include if inactiveDate is ON or AFTER periodStart
                        return !emp.getInactiveDate().isBefore(periodStart);
                    })
                    .map(emp -> {
                        PayrollEntry entry = new PayrollEntry();
                        entry.setPeriod(period);
                        entry.setEmployee(emp);
                        return entry;
                    }).collect(Collectors.toList());
            entryRepository.saveAll(entries);
        }
        // Sort entries by Member ID for consistent display
        entries.sort((e1, e2) -> {
            try {
                return Integer.compare(Integer.parseInt(e1.getEmployee().getMemberId()),
                        Integer.parseInt(e2.getEmployee().getMemberId()));
            } catch (NumberFormatException e) {
                return e1.getEmployee().getMemberId().compareTo(e2.getEmployee().getMemberId());
            }
        });

        return entries;
    }

    // 3. Save Single Row (Calculation Trigger)
    @PutMapping("/entries/{entryId}")
    public PayrollEntry updateEntry(@PathVariable UUID entryId, @RequestBody PayrollEntry input) {
        PayrollEntry existing = entryRepository.findById(entryId).orElseThrow();

        // Update ONLY inputs (Security/ReadOnly constraint)
        existing.setDaysWorked(input.getDaysWorked());
        existing.setWagesEarned(input.getWagesEarned());
        existing.setAdvanceDeduction(input.getAdvanceDeduction());
        // Fix: Save active days for Casual Labour
        if (input.getActiveDays() != null) {
            existing.setActiveDays(input.getActiveDays());
        }

        // Calculate
        calculatorService.calculate(existing);

        return entryRepository.save(existing);
    }

    // 3.1 Bulk Update (Calculate All)
    @PutMapping("/entries")
    public java.util.List<PayrollEntry> bulkUpdateEntries(@RequestBody java.util.List<PayrollEntry> inputs) {
        java.util.List<PayrollEntry> updated = new java.util.ArrayList<>();

        for (PayrollEntry input : inputs) {
            java.util.Optional<PayrollEntry> opt = entryRepository.findById(input.getId());
            if (opt.isPresent()) {
                PayrollEntry existing = opt.get();
                // existing.setDaysWorked(input.getDaysWorked()); // Calculator will override
                // this based on activeDays if present
                // But if activeDays is empty (Head Load), we trust input.daysWorked?
                // Let's keep setting it, but Calculator takes precedence if activeDays exists.
                existing.setDaysWorked(input.getDaysWorked());

                existing.setWagesEarned(input.getWagesEarned());
                existing.setAdvanceDeduction(input.getAdvanceDeduction());

                // Fix: Save active days
                if (input.getActiveDays() != null) {
                    existing.setActiveDays(input.getActiveDays());
                }

                calculatorService.calculate(existing);
                updated.add(existing);
            }
        }

        return entryRepository.saveAll(updated);
    }

    // 4. Finalize/Close Period
    @PostMapping("/periods/{periodId}/close")
    public PayrollPeriod closePeriod(@PathVariable UUID periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        period.setStatus(PayrollPeriod.Status.CLOSED);
        return periodRepository.save(period);
    }

    // 4.1 Reopen/Unlock Period
    @PostMapping("/periods/{periodId}/reopen")
    public PayrollPeriod reopenPeriod(@PathVariable UUID periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        period.setStatus(PayrollPeriod.Status.OPEN);
        return periodRepository.save(period);
    }

    // 5. Get Period Details
    @GetMapping("/periods/{periodIdStr}")
    public PayrollPeriod getPeriod(@PathVariable String periodIdStr) {
        if ("latest".equalsIgnoreCase(periodIdStr)) {
            // Find latest by ordering (Year DESC, Month DESC)
            return periodRepository.findAllByOrderByYearDescMonthDesc().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        java.time.LocalDate now = java.time.LocalDate.now();
                        PayrollPeriod p = new PayrollPeriod();
                        p.setMonth(now.getMonthValue());
                        p.setYear(now.getYear());
                        p.setStatus(PayrollPeriod.Status.OPEN);
                        return periodRepository.save(p);
                    });
        }
        return periodRepository.findById(UUID.fromString(periodIdStr)).orElseThrow();
    }

    @Autowired
    private com.fci.automation.service.PayrollImportService importService;

    // 6. Data Import
    @GetMapping("/import/template")
    public org.springframework.http.ResponseEntity<byte[]> downloadTemplate() {
        byte[] excel = importService.generateTemplate();
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Payroll_Import_Template.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @PostMapping("/import/{periodId}")
    public org.springframework.http.ResponseEntity<?> importPayroll(
            @PathVariable UUID periodId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String result = importService.importPayroll(file, periodId);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Import failed: " + e.getMessage()));
        }
    }

    // 7. Import UTR Data
    @PostMapping("/import/utr/{periodId}")
    public org.springframework.http.ResponseEntity<?> importUtr(
            @PathVariable UUID periodId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String result = importService.importUtrData(file, periodId);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Import failed: " + e.getMessage()));
        }
    }
}
