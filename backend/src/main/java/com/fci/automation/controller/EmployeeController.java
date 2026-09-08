package com.fci.automation.controller;

import com.fci.automation.dto.EmployeeImportSummary;
import com.fci.automation.dto.ImportPreviewResult;
import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.service.EmployeeImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular dev server
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeImportService employeeImportService;

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(org.springframework.dao.DataIntegrityViolationException e) {
        String message = "Duplicate entry detected.";
        String msgStr = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
        if (msgStr.contains("MEMBER_ID"))
            message = "Member ID already exists.";
        if (msgStr.contains("UAN_NUMBER"))
            message = "UAN Number already exists.";
        if (msgStr.contains("IP_NUMBER"))
            message = "IP Number already exists.";
        if (msgStr.contains("BANK_ACCOUNT_NO"))
            message = "Bank Account Number already exists.";
        if (msgStr.contains("AADHAAR_NUMBER"))
            message = "Aadhaar Number already exists.";
        if (msgStr.contains("PAN_NUMBER"))
            message = "PAN Number already exists.";

        return Map.of("message", message + " (Value already used)");
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .sorted((e1, e2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(e1.getMemberId()), Integer.parseInt(e2.getMemberId()));
                    } catch (NumberFormatException e) {
                        return e1.getMemberId().compareTo(e2.getMemberId());
                    }
                })
                .collect(Collectors.toList());
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable UUID id, @RequestBody Employee employee) {
        employee.setId(id);
        return employeeRepository.save(employee);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeImportSummary uploadEmployees(@RequestParam("file") MultipartFile file) {
        return employeeImportService.importEmployees(file);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreviewResult previewImport(@RequestParam("file") MultipartFile file) {
        return employeeImportService.previewImport(file);
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeImportSummary applyImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "approvedMemberIds", required = false, defaultValue = "") String approvedMemberIds,
            @RequestParam(value = "addAsNewMemberIds", required = false, defaultValue = "") String addAsNewMemberIds) {
        Set<String> approved = approvedMemberIds.isBlank() ? new HashSet<>() :
                new HashSet<>(Arrays.asList(approvedMemberIds.split(",")));
        Set<String> addAsNew = addAsNewMemberIds.isBlank() ? new HashSet<>() :
                new HashSet<>(Arrays.asList(addAsNewMemberIds.split(",")));
        return employeeImportService.applyImport(file, approved, addAsNew);
    }

    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable UUID id) {
        employeeRepository.deleteById(id);
    }

    @Autowired
    private com.fci.automation.config.DataSeeder dataSeeder;

    @PostMapping("/reset-db")
    public Map<String, String> resetDatabase() {
        try {
            // Always reset the REAL realm — RealmContext is not available during HTTP requests
            com.fci.automation.config.RealmContext.setRealm(com.fci.automation.config.RealmEnum.REAL);
            dataSeeder.resetAndSeedDatabase(com.fci.automation.config.RealmEnum.REAL);
            return Map.of("status", "success", "message", "Database reset and seeded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        } finally {
            com.fci.automation.config.RealmContext.clear();
        }
    }
}
