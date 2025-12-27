package com.fci.automation.controller;

import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular dev server
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public java.util.Map<String, String> handleConflict(org.springframework.dao.DataIntegrityViolationException e) {
        String message = "Duplicate entry detected.";
        if (e.getMessage().contains("MEMBER_ID"))
            message = "Member ID already exists.";
        if (e.getMessage().contains("UAN_NUMBER"))
            message = "UAN Number already exists.";
        if (e.getMessage().contains("IP_NUMBER"))
            message = "IP Number already exists.";
        if (e.getMessage().contains("BANK_ACCOUNT_NO"))
            message = "Bank Account Number already exists.";

        // Fallback for H2 generic messages if specific constraint names aren't parsed
        // easily
        return java.util.Map.of("message", message + " (Value already used)");
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        // Sort by Member ID properly (parsing to int if possible for numeric sort, else
        // string sort)
        return employeeRepository.findAll().stream()
                .sorted((e1, e2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(e1.getMemberId()), Integer.parseInt(e2.getMemberId()));
                    } catch (NumberFormatException e) {
                        return e1.getMemberId().compareTo(e2.getMemberId());
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable UUID id, @RequestBody Employee employee) {
        employee.setId(id); // Ensure ID matches
        return employeeRepository.save(employee);
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Employee> uploadEmployees(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream inputStream = file.getInputStream();
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                        inputStream)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            java.util.List<Employee> employees = new java.util.ArrayList<>();

            // 1. Parse Header Row (Row 0)
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                throw new RuntimeException("Excel file is empty or missing headers.");

            java.util.Map<String, Integer> colMap = new java.util.HashMap<>();
            java.util.List<String> foundHeaders = new java.util.ArrayList<>();
            for (org.apache.poi.ss.usermodel.Cell cell : headerRow) {
                String header = getCellValue(cell).toLowerCase().trim();
                colMap.put(header, cell.getColumnIndex());
                foundHeaders.add(header);
            }

            // Identify Indices
            int memberIdIdx = findColIndex(colMap, "member id", "memberid", "member_id", "emp id", "employee id", "id");
            int nameIdx = findColIndex(colMap, "name", "full name", "fullname", "employee name");
            int uanIdx = findColIndex(colMap, "uan", "uan number", "uan_number");
            int ipIdx = findColIndex(colMap, "ip", "ip number", "ip_number");
            int bankIdx = findColIndex(colMap, "bank", "bank account", "account no", "ac no");
            int ifscIdx = findColIndex(colMap, "ifsc", "ifsc code");
            int catIdx = findColIndex(colMap, "category", "cat");

            if (memberIdIdx == -1)
                throw new RuntimeException("Missing required column: Member ID. Found headers: " + foundHeaders);

            // 2. Iterate Data Rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String memberId = getCellValue(row.getCell(memberIdIdx));
                if (memberId == null || memberId.trim().isEmpty())
                    continue;

                // Upsert Logic
                Employee emp = employeeRepository.findByMemberId(memberId)
                        .orElse(new Employee());

                if (emp.getId() == null) {
                    emp.setMemberId(memberId);
                    emp.setStatus(Employee.Status.ACTIVE);
                }

                if (nameIdx != -1)
                    emp.setFullName(getCellValue(row.getCell(nameIdx)));
                if (uanIdx != -1) {
                    String uan = getCellValue(row.getCell(uanIdx));
                    if (uan != null && !uan.isEmpty()) {
                        // Check Conflict
                        employeeRepository.findByUanNumber(uan).ifPresent(existing -> {
                            if (!existing.getMemberId().equals(emp.getMemberId())) {
                                throw new RuntimeException("Row " + (row.getRowNum() + 1) + ": UAN " + uan
                                        + " is already used by Member ID " + existing.getMemberId());
                            }
                        });
                        emp.setUanNumber(uan);
                    }
                }

                if (ipIdx != -1) {
                    String ip = getCellValue(row.getCell(ipIdx));
                    if (ip != null && !ip.isEmpty()) {
                        employeeRepository.findByIpNumber(ip).ifPresent(existing -> {
                            if (!existing.getMemberId().equals(emp.getMemberId())) {
                                throw new RuntimeException("Row " + (row.getRowNum() + 1) + ": IP " + ip
                                        + " is already used by Member ID " + existing.getMemberId());
                            }
                        });
                        emp.setIpNumber(ip);
                    }
                }

                if (bankIdx != -1) {
                    String bank = getCellValue(row.getCell(bankIdx));
                    if (bank != null && !bank.isEmpty()) {
                        employeeRepository.findByBankAccountNo(bank).ifPresent(existing -> {
                            if (!existing.getMemberId().equals(emp.getMemberId())) {
                                throw new RuntimeException("Row " + (row.getRowNum() + 1) + ": Bank Account " + bank
                                        + " is already used by Member ID " + existing.getMemberId());
                            }
                        });
                        emp.setBankAccountNo(bank);
                    }
                }

                if (ifscIdx != -1) {
                    emp.setIfscCode(getCellValue(row.getCell(ifscIdx)));
                }

                // Category
                if (catIdx != -1) {
                    String catStr = getCellValue(row.getCell(catIdx));
                    try {
                        if (catStr != null && !catStr.isEmpty()) {
                            emp.setCategory(Employee.Category.valueOf(catStr.toUpperCase().trim()));
                        } else {
                            if (emp.getCategory() == null)
                                emp.setCategory(Employee.Category.CL);
                        }
                    } catch (IllegalArgumentException e) {
                        if (emp.getCategory() == null)
                            emp.setCategory(Employee.Category.CL);
                    }
                } else {
                    if (emp.getCategory() == null)
                        emp.setCategory(Employee.Category.CL);
                }

                employees.add(emp);
            }

            return employeeRepository.saveAll(employees);

        } catch (Exception e) {
            e.printStackTrace();
            // DEBUG: Return column mapping info in error to verify dynamic logic is active
            throw new RuntimeException("Upload Failed (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private int findColIndex(java.util.Map<String, Integer> map, String... keys) {
        for (String key : keys) {
            for (String mapKey : map.keySet()) {
                if (mapKey.contains(key))
                    return map.get(mapKey);
            }
        }
        return -1;
    }

    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue()); // Integer only for IDs
            default:
                return "";
        }
    }

    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable UUID id) {
        employeeRepository.deleteById(id);
    }
}
