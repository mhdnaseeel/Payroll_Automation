package com.fci.automation.controller;

import com.fci.automation.entity.DailyBagsWork;
import com.fci.automation.entity.DailyClauseWork;
import com.fci.automation.entity.DailyHeadcount;
import com.fci.automation.service.SalaryService;
import com.fci.automation.service.SalaryService.SalaryCalculationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/salary")
@CrossOrigin(origins = "http://localhost:4200")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    // 1. Submit Table 1 Data (Bags Stacked heights)
    @PostMapping("/bags")
    public ResponseEntity<?> saveBagsWork(@RequestBody DailyBagsWork work) {
        if (work.getDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Date is required."));
        }
        try {
            DailyBagsWork saved = salaryService.saveBagsWork(work);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error saving bags data: " + e.getMessage()));
        }
    }

    // 2. Submit Table 4 Data (Clause 15 Issue)
    @PostMapping("/clause")
    public ResponseEntity<?> saveClauseWork(@RequestBody DailyClauseWork work) {
        if (work.getDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Date is required."));
        }
        try {
            DailyClauseWork saved = salaryService.saveClauseWork(work);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error saving clause data: " + e.getMessage()));
        }
    }

    // 2.5 Submit Table 2 Data (Manual Active Headcount Override)
    @PostMapping("/headcount")
    public ResponseEntity<?> saveHeadcount(@RequestBody DailyHeadcount headcount) {
        if (headcount.getDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Date is required."));
        }
        if (headcount.getHeadcount() == null || headcount.getHeadcount() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Valid headcount is required."));
        }
        try {
            DailyHeadcount saved = salaryService.saveHeadcount(headcount);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error saving headcount override: " + e.getMessage()));
        }
    }

    // 3. Get Calculation for a specific Date (Table 5 Preview)
    @GetMapping("/daily/{date}")
    public ResponseEntity<?> getDailyCalculation(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            SalaryCalculationResult result = salaryService.calculateDailySalary(date);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error calculating daily salary: " + e.getMessage()));
        }
    }

    // 4. Get Calculation for a date range (Viewer)
    @GetMapping("/range")
    public ResponseEntity<?> getSalaryRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            java.util.List<SalaryCalculationResult> results = salaryService.calculateSalaryRange(start, end);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error calculating salary range: " + e.getMessage()));
        }
    }
}
