package com.fci.automation.service;

import com.fci.automation.dto.EmployeeImportSummary;
import com.fci.automation.dto.ImportPreviewResult;
import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EmployeeImportService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional
    public EmployeeImportSummary importEmployees(MultipartFile file) {
        EmployeeImportSummary summary = new EmployeeImportSummary();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is empty or missing headers.");
            }

            Map<String, Integer> colMap = new HashMap<>();
            List<String> foundHeaders = new ArrayList<>();
            for (Cell cell : headerRow) {
                String header = getCellValue(cell).toLowerCase().trim();
                colMap.put(header, cell.getColumnIndex());
                foundHeaders.add(header);
            }

            int memberIdIdx = findColIndex(colMap, "member id", "memberid", "member_id", "emp id", "employee id", "emp code", "employee code", "code", "id");
            int nameIdx = findColIndex(colMap, "name", "full name", "fullname", "employee name", "emp name");
            int uanIdx = findColIndex(colMap, "uan", "uan number", "uan_number", "uan no");
            int ipIdx = findColIndex(colMap, "ip", "ip number", "ip_number", "ip no", "esi", "esi no", "esi number");
            int bankIdx = findColIndex(colMap, "bank", "bank account", "account no", "ac no", "acc no", "bank ac no");
            int ifscIdx = findColIndex(colMap, "ifsc", "ifsc code");
            int catIdx = findColIndex(colMap, "category", "cat");
            int aadhaarIdx = findColIndex(colMap, "aadhaar", "aadhaar number", "aadhaar_number", "aadhaar no", "aadhar");
            int panIdx = findColIndex(colMap, "pan", "pan number", "pan_number", "pan no");

            if (memberIdIdx == -1 && uanIdx == -1 && ipIdx == -1 && aadhaarIdx == -1 && panIdx == -1) {
                throw new IllegalArgumentException("Excel header missing unique identifier column (Member ID / UAN / IP / Aadhaar / PAN). Found: " + foundHeaders);
            }

            // In-batch lookup map for employees modified/created during this import session
            // Keyed by employee UUID (once identified/created)
            Map<UUID, Employee> batchEmployees = new HashMap<>();

            int totalRows = 0;
            int createdCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            int failedCount = 0;

            int lastRowNum = sheet.getLastRowNum();

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRowNumber = r + 1;

                String memberId = memberIdIdx != -1 ? cleanString(getCellValue(row.getCell(memberIdIdx))) : null;
                String name = nameIdx != -1 ? cleanString(getCellValue(row.getCell(nameIdx))) : null;
                String uan = uanIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(uanIdx))) : null;
                String ip = ipIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(ipIdx))) : null;
                String bank = bankIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(bankIdx))) : null;
                String ifsc = ifscIdx != -1 ? cleanString(getCellValue(row.getCell(ifscIdx))) : null;
                String catStr = catIdx != -1 ? cleanString(getCellValue(row.getCell(catIdx))) : null;
                String aadhaar = aadhaarIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(aadhaarIdx))) : null;
                String pan = panIdx != -1 ? cleanPan(getCellValue(row.getCell(panIdx))) : null;

                // 1. Validate Mandatory Fields
                if (memberId == null || memberId.isEmpty()) {
                    failedCount++;
                    summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                            displayRowNumber, memberId, name, "Missing mandatory field: Employee Code / Member ID"));
                    continue;
                }

                if (name == null || name.isEmpty()) {
                    failedCount++;
                    summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                            displayRowNumber, memberId, name, "Missing mandatory field: Employee Name"));
                    continue;
                }

                // Parse Category
                Employee.Category category = Employee.Category.CL;
                if (catStr != null && !catStr.isEmpty()) {
                    try {
                        category = Employee.Category.valueOf(catStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        category = Employee.Category.CL;
                    }
                }

                // 2. Find Existing Employee matching identifiers
                Set<Employee> matchedExisting = new HashSet<>();

                // Match by Member ID
                findInBatchOrDbByMemberId(memberId, batchEmployees).ifPresent(matchedExisting::add);
                // Match by UAN
                if (uan != null) findInBatchOrDbByUan(uan, batchEmployees).ifPresent(matchedExisting::add);
                // Match by IP
                if (ip != null) findInBatchOrDbByIp(ip, batchEmployees).ifPresent(matchedExisting::add);
                // Match by Aadhaar
                if (aadhaar != null) findInBatchOrDbByAadhaar(aadhaar, batchEmployees).ifPresent(matchedExisting::add);
                // Match by PAN
                if (pan != null) findInBatchOrDbByPan(pan, batchEmployees).ifPresent(matchedExisting::add);

                // Check conflict: multiple distinct employees matched
                if (matchedExisting.size() > 1) {
                    failedCount++;
                    summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                            displayRowNumber, memberId, name, "Conflict: Row identifiers match multiple different existing records"));
                    continue;
                }

                Employee matchedEmp = matchedExisting.isEmpty() ? null : matchedExisting.iterator().next();

                // 3. Identifier Conflict Check with THIRD employees
                if (matchedEmp != null) {
                    // Check if memberId conflicts with another employee
                    if (isConflictMemberId(memberId, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Member ID " + memberId + " is already used by another employee"));
                        continue;
                    }
                    if (uan != null && isConflictUan(uan, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "UAN " + uan + " is already used by another employee"));
                        continue;
                    }
                    if (ip != null && isConflictIp(ip, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "IP Number " + ip + " is already used by another employee"));
                        continue;
                    }
                    if (bank != null && isConflictBank(bank, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Bank Account " + bank + " is already used by another employee"));
                        continue;
                    }
                    if (aadhaar != null && isConflictAadhaar(aadhaar, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Aadhaar Number " + aadhaar + " is already used by another employee"));
                        continue;
                    }
                    if (pan != null && isConflictPan(pan, matchedEmp.getId(), batchEmployees)) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "PAN Number " + pan + " is already used by another employee"));
                        continue;
                    }

                    // Perform Update vs Skip diff check
                    boolean isModified = false;
                    List<EmployeeImportSummary.FieldChange> changes = new ArrayList<>();

                    if (!Objects.equals(matchedEmp.getMemberId(), memberId)) {
                        changes.add(new EmployeeImportSummary.FieldChange("Member ID", matchedEmp.getMemberId(), memberId));
                        matchedEmp.setMemberId(memberId);
                        isModified = true;
                    }
                    if (!Objects.equals(matchedEmp.getFullName(), name)) {
                        changes.add(new EmployeeImportSummary.FieldChange("Name", matchedEmp.getFullName(), name));
                        matchedEmp.setFullName(name);
                        isModified = true;
                    }
                    if (uan != null && !Objects.equals(matchedEmp.getUanNumber(), uan)) {
                        changes.add(new EmployeeImportSummary.FieldChange("UAN", matchedEmp.getUanNumber(), uan));
                        matchedEmp.setUanNumber(uan);
                        isModified = true;
                    }
                    if (ip != null && !Objects.equals(matchedEmp.getIpNumber(), ip)) {
                        changes.add(new EmployeeImportSummary.FieldChange("IP Number", matchedEmp.getIpNumber(), ip));
                        matchedEmp.setIpNumber(ip);
                        isModified = true;
                    }
                    if (bank != null && !Objects.equals(matchedEmp.getBankAccountNo(), bank)) {
                        changes.add(new EmployeeImportSummary.FieldChange("Bank Account", matchedEmp.getBankAccountNo(), bank));
                        matchedEmp.setBankAccountNo(bank);
                        isModified = true;
                    }
                    if (ifsc != null && !Objects.equals(matchedEmp.getIfscCode(), ifsc)) {
                        changes.add(new EmployeeImportSummary.FieldChange("IFSC Code", matchedEmp.getIfscCode(), ifsc));
                        matchedEmp.setIfscCode(ifsc);
                        isModified = true;
                    }
                    if (category != null && matchedEmp.getCategory() != category) {
                        changes.add(new EmployeeImportSummary.FieldChange("Category",
                                matchedEmp.getCategory() != null ? matchedEmp.getCategory().name() : "-", category.name()));
                        matchedEmp.setCategory(category);
                        isModified = true;
                    }
                    if (aadhaar != null && !Objects.equals(matchedEmp.getAadhaarNumber(), aadhaar)) {
                        changes.add(new EmployeeImportSummary.FieldChange("Aadhaar", matchedEmp.getAadhaarNumber(), aadhaar));
                        matchedEmp.setAadhaarNumber(aadhaar);
                        isModified = true;
                    }
                    if (pan != null && !Objects.equals(matchedEmp.getPanNumber(), pan)) {
                        changes.add(new EmployeeImportSummary.FieldChange("PAN", matchedEmp.getPanNumber(), pan));
                        matchedEmp.setPanNumber(pan);
                        isModified = true;
                    }

                    if (isModified) {
                        Employee saved = employeeRepository.save(matchedEmp);
                        batchEmployees.put(saved.getId(), saved);
                        updatedCount++;
                        summary.getUpdatedRecords().add(new EmployeeImportSummary.ImportUpdateDetail(
                                displayRowNumber, memberId, name, changes));
                    } else {
                        skippedCount++;
                    }

                } else {
                    // Check conflicts for NEW employee
                    if (findInBatchOrDbByMemberId(memberId, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Member ID " + memberId + " is already used"));
                        continue;
                    }
                    if (uan != null && findInBatchOrDbByUan(uan, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "UAN " + uan + " is already used"));
                        continue;
                    }
                    if (ip != null && findInBatchOrDbByIp(ip, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "IP Number " + ip + " is already used"));
                        continue;
                    }
                    if (bank != null && findInBatchOrDbByBank(bank, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Bank Account " + bank + " is already used"));
                        continue;
                    }
                    if (aadhaar != null && findInBatchOrDbByAadhaar(aadhaar, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "Aadhaar Number " + aadhaar + " is already used"));
                        continue;
                    }
                    if (pan != null && findInBatchOrDbByPan(pan, batchEmployees).isPresent()) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                                displayRowNumber, memberId, name, "PAN Number " + pan + " is already used"));
                        continue;
                    }

                    Employee newEmp = new Employee();
                    newEmp.setMemberId(memberId);
                    newEmp.setFullName(name);
                    newEmp.setUanNumber(uan);
                    newEmp.setIpNumber(ip);
                    newEmp.setBankAccountNo(bank);
                    newEmp.setIfscCode(ifsc);
                    newEmp.setCategory(category);
                    newEmp.setAadhaarNumber(aadhaar);
                    newEmp.setPanNumber(pan);
                    newEmp.setStatus(Employee.Status.ACTIVE);
                    newEmp.setCreatedAt(LocalDateTime.now());

                    Employee saved = employeeRepository.save(newEmp);
                    batchEmployees.put(saved.getId(), saved);
                    createdCount++;
                }
            }

            summary.setTotalRecords(totalRows);
            summary.setCreatedCount(createdCount);
            summary.setUpdatedCount(updatedCount);
            summary.setSkippedCount(skippedCount);
            summary.setFailedCount(failedCount);

            return summary;

        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    private Optional<Employee> findInBatchOrDbByMemberId(String memberId, Map<UUID, Employee> batch) {
        if (memberId == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (memberId.equalsIgnoreCase(emp.getMemberId())) return Optional.of(emp);
        }
        return employeeRepository.findByMemberId(memberId);
    }

    private Optional<Employee> findInBatchOrDbByUan(String uan, Map<UUID, Employee> batch) {
        if (uan == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (uan.equalsIgnoreCase(emp.getUanNumber())) return Optional.of(emp);
        }
        return employeeRepository.findByUanNumber(uan);
    }

    private Optional<Employee> findInBatchOrDbByIp(String ip, Map<UUID, Employee> batch) {
        if (ip == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (ip.equalsIgnoreCase(emp.getIpNumber())) return Optional.of(emp);
        }
        return employeeRepository.findByIpNumber(ip);
    }

    private Optional<Employee> findInBatchOrDbByBank(String bank, Map<UUID, Employee> batch) {
        if (bank == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (bank.equalsIgnoreCase(emp.getBankAccountNo())) return Optional.of(emp);
        }
        return employeeRepository.findByBankAccountNo(bank);
    }

    private Optional<Employee> findInBatchOrDbByAadhaar(String aadhaar, Map<UUID, Employee> batch) {
        if (aadhaar == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (aadhaar.equalsIgnoreCase(emp.getAadhaarNumber())) return Optional.of(emp);
        }
        return employeeRepository.findByAadhaarNumber(aadhaar);
    }

    private Optional<Employee> findInBatchOrDbByPan(String pan, Map<UUID, Employee> batch) {
        if (pan == null) return Optional.empty();
        for (Employee emp : batch.values()) {
            if (pan.equalsIgnoreCase(emp.getPanNumber())) return Optional.of(emp);
        }
        return employeeRepository.findByPanNumber(pan);
    }

    private boolean isConflictMemberId(String memberId, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByMemberId(memberId, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private boolean isConflictUan(String uan, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByUan(uan, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private boolean isConflictIp(String ip, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByIp(ip, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private boolean isConflictBank(String bank, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByBank(bank, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private boolean isConflictAadhaar(String aadhaar, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByAadhaar(aadhaar, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private boolean isConflictPan(String pan, UUID selfId, Map<UUID, Employee> batch) {
        Optional<Employee> found = findInBatchOrDbByPan(pan, batch);
        return found.isPresent() && !found.get().getId().equals(selfId);
    }

    private int findColIndex(Map<String, Integer> map, String... keys) {
        for (String key : keys) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entry.getKey().contains(key)) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private String cleanString(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizeDigits(String str) {
        if (str == null) return null;
        String cleaned = str.replaceAll("[^0-9]", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String cleanPan(String str) {
        if (str == null) return null;
        String cleaned = str.toUpperCase().replaceAll("[^A-Z0-9]", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    // ─── PREVIEW (dry-run, no DB changes) ─────────────────────────────────────────

    public ImportPreviewResult previewImport(MultipartFile file) {
        ImportPreviewResult preview = new ImportPreviewResult();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel file is empty or missing headers.");

            Map<String, Integer> colMap = buildColMap(headerRow);
            int[] cols = resolveColumns(colMap);
            int memberIdIdx = cols[0], nameIdx = cols[1], uanIdx = cols[2], ipIdx = cols[3],
                    bankIdx = cols[4], ifscIdx = cols[5], catIdx = cols[6], aadhaarIdx = cols[7], panIdx = cols[8];

            if (memberIdIdx == -1 && uanIdx == -1 && ipIdx == -1 && aadhaarIdx == -1 && panIdx == -1)
                throw new IllegalArgumentException("Excel header missing unique identifier column.");

            // Use a read-only batch map — no actual employees added
            Map<UUID, Employee> fakeBatch = new HashMap<>();
            int totalRows = 0;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                totalRows++;
                int displayRowNumber = r + 1;

                String memberId = memberIdIdx != -1 ? cleanString(getCellValue(row.getCell(memberIdIdx))) : null;
                String name = nameIdx != -1 ? cleanString(getCellValue(row.getCell(nameIdx))) : null;
                String uan = uanIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(uanIdx))) : null;
                String ip = ipIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(ipIdx))) : null;
                String bank = bankIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(bankIdx))) : null;
                String ifsc = ifscIdx != -1 ? cleanString(getCellValue(row.getCell(ifscIdx))) : null;
                String catStr = catIdx != -1 ? cleanString(getCellValue(row.getCell(catIdx))) : null;
                String aadhaar = aadhaarIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(aadhaarIdx))) : null;
                String pan = panIdx != -1 ? cleanPan(getCellValue(row.getCell(panIdx))) : null;

                if (memberId == null || memberId.isEmpty()) {
                    preview.getFailed().add(new EmployeeImportSummary.ImportFailureDetail(displayRowNumber, memberId, name, "Missing mandatory field: Employee Code / Member ID"));
                    continue;
                }
                if (name == null || name.isEmpty()) {
                    preview.getFailed().add(new EmployeeImportSummary.ImportFailureDetail(displayRowNumber, memberId, name, "Missing mandatory field: Employee Name"));
                    continue;
                }

                Employee.Category category = Employee.Category.CL;
                if (catStr != null && !catStr.isEmpty()) {
                    try { category = Employee.Category.valueOf(catStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }

                Set<Employee> matchedExisting = new HashSet<>();
                findInBatchOrDbByMemberId(memberId, fakeBatch).ifPresent(matchedExisting::add);
                if (uan != null) findInBatchOrDbByUan(uan, fakeBatch).ifPresent(matchedExisting::add);
                if (ip != null) findInBatchOrDbByIp(ip, fakeBatch).ifPresent(matchedExisting::add);
                if (aadhaar != null) findInBatchOrDbByAadhaar(aadhaar, fakeBatch).ifPresent(matchedExisting::add);
                if (pan != null) findInBatchOrDbByPan(pan, fakeBatch).ifPresent(matchedExisting::add);

                if (matchedExisting.size() > 1) {
                    preview.getFailed().add(new EmployeeImportSummary.ImportFailureDetail(displayRowNumber, memberId, name, "Conflict: identifiers match multiple existing records"));
                    continue;
                }

                Employee matchedEmp = matchedExisting.isEmpty() ? null : matchedExisting.iterator().next();

                if (matchedEmp != null) {
                    // Check conflicts
                    if (isConflictMemberId(memberId, matchedEmp.getId(), fakeBatch)) {
                        preview.getFailed().add(new EmployeeImportSummary.ImportFailureDetail(displayRowNumber, memberId, name, "Member ID " + memberId + " is already used by another employee"));
                        continue;
                    }

                    // Compute diff
                    List<EmployeeImportSummary.FieldChange> changes = new ArrayList<>();
                    if (!Objects.equals(matchedEmp.getMemberId(), memberId)) changes.add(new EmployeeImportSummary.FieldChange("Member ID", matchedEmp.getMemberId(), memberId));
                    if (!Objects.equals(matchedEmp.getFullName(), name)) changes.add(new EmployeeImportSummary.FieldChange("Name", matchedEmp.getFullName(), name));
                    if (uan != null && !Objects.equals(matchedEmp.getUanNumber(), uan)) changes.add(new EmployeeImportSummary.FieldChange("UAN", matchedEmp.getUanNumber(), uan));
                    if (ip != null && !Objects.equals(matchedEmp.getIpNumber(), ip)) changes.add(new EmployeeImportSummary.FieldChange("IP Number", matchedEmp.getIpNumber(), ip));
                    if (bank != null && !Objects.equals(matchedEmp.getBankAccountNo(), bank)) changes.add(new EmployeeImportSummary.FieldChange("Bank Account", matchedEmp.getBankAccountNo(), bank));
                    if (ifsc != null && !Objects.equals(matchedEmp.getIfscCode(), ifsc)) changes.add(new EmployeeImportSummary.FieldChange("IFSC Code", matchedEmp.getIfscCode(), ifsc));
                    if (category != null && matchedEmp.getCategory() != category) changes.add(new EmployeeImportSummary.FieldChange("Category", matchedEmp.getCategory() != null ? matchedEmp.getCategory().name() : "-", category.name()));
                    if (aadhaar != null && !Objects.equals(matchedEmp.getAadhaarNumber(), aadhaar)) changes.add(new EmployeeImportSummary.FieldChange("Aadhaar", matchedEmp.getAadhaarNumber(), aadhaar));
                    if (pan != null && !Objects.equals(matchedEmp.getPanNumber(), pan)) changes.add(new EmployeeImportSummary.FieldChange("PAN", matchedEmp.getPanNumber(), pan));

                    if (!changes.isEmpty()) {
                        preview.getToUpdate().add(new ImportPreviewResult.UpdateEmployeePreview(displayRowNumber, memberId, name, changes));
                    } else {
                        preview.setSkippedCount(preview.getSkippedCount() + 1);
                    }
                } else {
                    // New employee
                    preview.getToCreate().add(new ImportPreviewResult.NewEmployeePreview(
                            displayRowNumber, memberId, name, uan, ip, bank, ifsc,
                            category != null ? category.name() : "CL"));
                }
            }

            preview.setTotalRows(totalRows);
            return preview;

        } catch (Exception e) {
            throw new RuntimeException("Preview failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public EmployeeImportSummary applyImport(MultipartFile file, Set<String> approvedMemberIds, Set<String> addAsNewMemberIds) {
        EmployeeImportSummary summary = new EmployeeImportSummary();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel file is empty or missing headers.");

            Map<String, Integer> colMap = buildColMap(headerRow);
            int[] cols = resolveColumns(colMap);
            int memberIdIdx = cols[0], nameIdx = cols[1], uanIdx = cols[2], ipIdx = cols[3],
                    bankIdx = cols[4], ifscIdx = cols[5], catIdx = cols[6], aadhaarIdx = cols[7], panIdx = cols[8];

            Map<UUID, Employee> batchEmployees = new HashMap<>();
            int totalRows = 0, createdCount = 0, updatedCount = 0, skippedCount = 0, failedCount = 0;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;
                totalRows++;
                int displayRowNumber = r + 1;

                String memberId = memberIdIdx != -1 ? cleanString(getCellValue(row.getCell(memberIdIdx))) : null;
                String name = nameIdx != -1 ? cleanString(getCellValue(row.getCell(nameIdx))) : null;
                String uan = uanIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(uanIdx))) : null;
                String ip = ipIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(ipIdx))) : null;
                String bank = bankIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(bankIdx))) : null;
                String ifsc = ifscIdx != -1 ? cleanString(getCellValue(row.getCell(ifscIdx))) : null;
                String catStr = catIdx != -1 ? cleanString(getCellValue(row.getCell(catIdx))) : null;
                String aadhaar = aadhaarIdx != -1 ? sanitizeDigits(getCellValue(row.getCell(aadhaarIdx))) : null;
                String pan = panIdx != -1 ? cleanPan(getCellValue(row.getCell(panIdx))) : null;

                if (memberId == null || memberId.isEmpty() || name == null || name.isEmpty()) {
                    failedCount++;
                    summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(
                            displayRowNumber, memberId, name, "Missing mandatory field"));
                    continue;
                }

                // Skip rows NOT in the approved list
                if (!approvedMemberIds.contains(memberId)) {
                    skippedCount++;
                    continue;
                }

                Employee.Category category = Employee.Category.CL;
                if (catStr != null && !catStr.isEmpty()) {
                    try { category = Employee.Category.valueOf(catStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }

                // Check if user chose to add this employee as brand NEW (even if Member ID matches an existing one)
                boolean forceNew = addAsNewMemberIds.contains(memberId);

                Employee matchedEmp = null;
                if (!forceNew) {
                    Set<Employee> matchedExisting = new HashSet<>();
                    findInBatchOrDbByMemberId(memberId, batchEmployees).ifPresent(matchedExisting::add);
                    if (uan != null) findInBatchOrDbByUan(uan, batchEmployees).ifPresent(matchedExisting::add);
                    if (ip != null) findInBatchOrDbByIp(ip, batchEmployees).ifPresent(matchedExisting::add);
                    if (aadhaar != null) findInBatchOrDbByAadhaar(aadhaar, batchEmployees).ifPresent(matchedExisting::add);
                    if (pan != null) findInBatchOrDbByPan(pan, batchEmployees).ifPresent(matchedExisting::add);

                    if (matchedExisting.size() > 1) {
                        failedCount++;
                        summary.getFailedRecords().add(new EmployeeImportSummary.ImportFailureDetail(displayRowNumber, memberId, name, "Conflict: identifiers match multiple existing records"));
                        continue;
                    }
                    if (!matchedExisting.isEmpty()) {
                        matchedEmp = matchedExisting.iterator().next();
                    }
                }

                if (matchedEmp != null) {
                    // Update approved existing employee
                    boolean isModified = false;
                    List<EmployeeImportSummary.FieldChange> changes = new ArrayList<>();
                    if (!Objects.equals(matchedEmp.getMemberId(), memberId)) { changes.add(new EmployeeImportSummary.FieldChange("Member ID", matchedEmp.getMemberId(), memberId)); matchedEmp.setMemberId(memberId); isModified = true; }
                    if (!Objects.equals(matchedEmp.getFullName(), name)) { changes.add(new EmployeeImportSummary.FieldChange("Name", matchedEmp.getFullName(), name)); matchedEmp.setFullName(name); isModified = true; }
                    if (uan != null && !Objects.equals(matchedEmp.getUanNumber(), uan)) { changes.add(new EmployeeImportSummary.FieldChange("UAN", matchedEmp.getUanNumber(), uan)); matchedEmp.setUanNumber(uan); isModified = true; }
                    if (ip != null && !Objects.equals(matchedEmp.getIpNumber(), ip)) { changes.add(new EmployeeImportSummary.FieldChange("IP Number", matchedEmp.getIpNumber(), ip)); matchedEmp.setIpNumber(ip); isModified = true; }
                    if (bank != null && !Objects.equals(matchedEmp.getBankAccountNo(), bank)) { changes.add(new EmployeeImportSummary.FieldChange("Bank Account", matchedEmp.getBankAccountNo(), bank)); matchedEmp.setBankAccountNo(bank); isModified = true; }
                    if (ifsc != null && !Objects.equals(matchedEmp.getIfscCode(), ifsc)) { changes.add(new EmployeeImportSummary.FieldChange("IFSC Code", matchedEmp.getIfscCode(), ifsc)); matchedEmp.setIfscCode(ifsc); isModified = true; }
                    if (category != null && matchedEmp.getCategory() != category) { changes.add(new EmployeeImportSummary.FieldChange("Category", matchedEmp.getCategory() != null ? matchedEmp.getCategory().name() : "-", category.name())); matchedEmp.setCategory(category); isModified = true; }
                    if (aadhaar != null && !Objects.equals(matchedEmp.getAadhaarNumber(), aadhaar)) { changes.add(new EmployeeImportSummary.FieldChange("Aadhaar", matchedEmp.getAadhaarNumber(), aadhaar)); matchedEmp.setAadhaarNumber(aadhaar); isModified = true; }
                    if (pan != null && !Objects.equals(matchedEmp.getPanNumber(), pan)) { changes.add(new EmployeeImportSummary.FieldChange("PAN", matchedEmp.getPanNumber(), pan)); matchedEmp.setPanNumber(pan); isModified = true; }

                    if (isModified) {
                        Employee saved = employeeRepository.save(matchedEmp);
                        batchEmployees.put(saved.getId(), saved);
                        updatedCount++;
                        summary.getUpdatedRecords().add(new EmployeeImportSummary.ImportUpdateDetail(displayRowNumber, memberId, name, changes));
                    } else {
                        skippedCount++;
                    }
                } else {
                    // Create new approved employee. If forceNew is true, generate a sequential Member ID.
                    Employee newEmp = new Employee();
                    String finalMemberId = memberId;
                    if (forceNew) {
                        finalMemberId = getNextMemberId(batchEmployees);
                    }
                    newEmp.setMemberId(finalMemberId); newEmp.setFullName(name); newEmp.setUanNumber(uan);
                    newEmp.setIpNumber(ip); newEmp.setBankAccountNo(bank); newEmp.setIfscCode(ifsc);
                    newEmp.setCategory(category); newEmp.setAadhaarNumber(aadhaar); newEmp.setPanNumber(pan);
                    newEmp.setStatus(Employee.Status.ACTIVE); newEmp.setCreatedAt(LocalDateTime.now());
                    Employee saved = employeeRepository.save(newEmp);
                    batchEmployees.put(saved.getId(), saved);
                    createdCount++;
                }
            }

            summary.setTotalRecords(totalRows);
            summary.setCreatedCount(createdCount);
            summary.setUpdatedCount(updatedCount);
            summary.setSkippedCount(skippedCount);
            summary.setFailedCount(failedCount);
            return summary;

        } catch (Exception e) {
            throw new RuntimeException("Apply failed: " + e.getMessage(), e);
        }
    }

    private String getNextMemberId(Map<UUID, Employee> batch) {
        int maxId = 0;
        for (Employee emp : employeeRepository.findAll()) {
            try {
                int id = Integer.parseInt(emp.getMemberId().trim());
                if (id > maxId) maxId = id;
            } catch (NumberFormatException ignored) {}
        }
        for (Employee emp : batch.values()) {
            try {
                int id = Integer.parseInt(emp.getMemberId().trim());
                if (id > maxId) maxId = id;
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(maxId + 1);
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────────

    private Map<String, Integer> buildColMap(Row headerRow) {
        Map<String, Integer> colMap = new HashMap<>();
        for (Cell cell : headerRow) colMap.put(getCellValue(cell).toLowerCase().trim(), cell.getColumnIndex());
        return colMap;
    }

    private int[] resolveColumns(Map<String, Integer> colMap) {
        return new int[] {
            findColIndex(colMap, "member id", "memberid", "member_id", "emp id", "employee id", "emp code", "employee code", "code", "id"),
            findColIndex(colMap, "name", "full name", "fullname", "employee name", "emp name"),
            findColIndex(colMap, "uan", "uan number", "uan_number", "uan no"),
            findColIndex(colMap, "ip", "ip number", "ip_number", "ip no", "esi", "esi no", "esi number"),
            findColIndex(colMap, "bank", "bank account", "account no", "ac no", "acc no", "bank ac no"),
            findColIndex(colMap, "ifsc", "ifsc code"),
            findColIndex(colMap, "category", "cat"),
            findColIndex(colMap, "aadhaar", "aadhaar number", "aadhaar_number", "aadhaar no", "aadhar"),
            findColIndex(colMap, "pan", "pan number", "pan_number", "pan no")
        };
    }
}
