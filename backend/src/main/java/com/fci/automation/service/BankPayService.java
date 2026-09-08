package com.fci.automation.service;

import com.fci.automation.dto.BankPayGenerateRequestDto;
import com.fci.automation.dto.BankPayImportResultDto;
import com.fci.automation.dto.BankPayRecordDto;
import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BankPayService {

    private final EmployeeRepository employeeRepository;

    public BankPayService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public BankPayImportResultDto parseAndMatchExcel(InputStream inputStream) {
        List<BankPayRecordDto> records = new ArrayList<>();
        Set<String> seenNamesInFile = new HashSet<>();

        int totalSuccess = 0;
        int totalFailed = 0;
        int sameBankCount = 0;
        BigDecimal sameBankAmount = BigDecimal.ZERO;
        int otherBankCount = 0;
        BigDecimal otherBankAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Fetch all employees from DB for lookup
        List<Employee> dbEmployees = employeeRepository.findAll();
        Map<String, Employee> employeeMap = new HashMap<>();
        for (Employee emp : dbEmployees) {
            if (emp.getFullName() != null) {
                employeeMap.put(emp.getFullName().trim().toLowerCase(), emp);
            }
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return new BankPayImportResultDto(Collections.emptyList(), 0, 0, 0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            int nameColIndex = 0;
            int amountColIndex = 1;
            boolean hasHeader = false;

            Row firstRow = sheet.getRow(sheet.getFirstRowNum());
            if (firstRow != null) {
                for (Cell cell : firstRow) {
                    String val = getCellStringValue(cell).trim().toLowerCase();
                    if (val.contains("name") || val.contains("employee")) {
                        nameColIndex = cell.getColumnIndex();
                        hasHeader = true;
                    } else if (val.contains("amount") || val.contains("pay") || val.contains("salary")) {
                        amountColIndex = cell.getColumnIndex();
                        hasHeader = true;
                    }
                }
            }

            int startRowIndex = hasHeader ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();

            for (int r = startRowIndex; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String empName = getCellStringValue(row.getCell(nameColIndex)).trim();
                String amountStr = getCellStringValue(row.getCell(amountColIndex)).trim();

                // Skip completely blank rows
                if (empName.isEmpty() && amountStr.isEmpty()) {
                    continue;
                }

                BankPayRecordDto record = new BankPayRecordDto();
                record.setEmployeeName(empName);

                // Parse Amount and round using standard mathematical rounding (HALF_UP)
                BigDecimal amount = parseAmount(row.getCell(amountColIndex));
                if (amount != null) {
                    amount = amount.setScale(0, java.math.RoundingMode.HALF_UP);
                } else {
                    amount = BigDecimal.ZERO;
                }
                record.setAmount(amount);

                // Check 1: Name presence
                if (empName.isEmpty()) {
                    record.setStatus("INVALID");
                    record.setReason("Employee Name is blank");
                    records.add(record);
                    totalFailed++;
                    continue;
                }

                String normalizedName = empName.toLowerCase();

                // Check 2: Duplicate name in Excel
                if (seenNamesInFile.contains(normalizedName)) {
                    record.setStatus("INVALID");
                    record.setReason("Duplicate employee name in Excel file");
                    records.add(record);
                    totalFailed++;
                    continue;
                }
                seenNamesInFile.add(normalizedName);

                // Check 3: Amount > 0
                boolean validAmount = amount.compareTo(BigDecimal.ZERO) > 0;

                // Check 4: Employee exists in DB
                Employee matchedEmployee = employeeMap.get(normalizedName);
                if (matchedEmployee == null) {
                    record.setStatus("NOT_FOUND");
                    record.setReason("Employee not found in Master database");
                    records.add(record);
                    totalFailed++;
                    continue;
                }

                // Populate matched details
                record.setEmployeeId(matchedEmployee.getMemberId());
                record.setAccountNumber(matchedEmployee.getBankAccountNo());
                record.setIfscCode(matchedEmployee.getIfscCode());

                // Check 5: Bank details validation
                if (!validAmount) {
                    record.setStatus("INVALID");
                    record.setReason("Amount must be greater than zero");
                    records.add(record);
                    totalFailed++;
                } else if (matchedEmployee.getBankAccountNo() == null || matchedEmployee.getBankAccountNo().trim().isEmpty()) {
                    record.setStatus("INVALID");
                    record.setReason("Missing Bank Account Number in database");
                    records.add(record);
                    totalFailed++;
                } else if (matchedEmployee.getIfscCode() == null || matchedEmployee.getIfscCode().trim().isEmpty()) {
                    record.setStatus("INVALID");
                    record.setReason("Missing IFSC Code in database");
                    records.add(record);
                    totalFailed++;
                } else {
                    record.setStatus("READY");
                    record.setReason("Ready for payment");

                    // Categorize Bank Type (Same Bank vs Other Bank)
                    String ifsc = matchedEmployee.getIfscCode().trim().toUpperCase();
                    if (ifsc.startsWith("SBIN")) {
                        record.setBankCategory("SAME_BANK");
                        sameBankCount++;
                        sameBankAmount = sameBankAmount.add(amount);
                    } else {
                        record.setBankCategory("OTHER_BANK");
                        otherBankCount++;
                        otherBankAmount = otherBankAmount.add(amount);
                    }

                    records.add(record);
                    totalSuccess++;
                    totalAmount = totalAmount.add(amount);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return new BankPayImportResultDto(
                records,
                records.size(),
                totalSuccess,
                totalFailed,
                sameBankCount,
                sameBankAmount,
                otherBankCount,
                otherBankAmount,
                totalAmount
        );
    }

    public String generateBankFile(BankPayGenerateRequestDto request) {
        StringBuilder txt = new StringBuilder();

        String companyAccount = (request.getCompanyAccount() != null && !request.getCompanyAccount().isBlank())
                ? request.getCompanyAccount().trim() : "44145351821";
        String branchCode = (request.getBranchCode() != null && !request.getBranchCode().isBlank())
                ? request.getBranchCode().trim() : "17242";
        String companyName = (request.getCompanyName() != null && !request.getCompanyName().isBlank())
                ? request.getCompanyName().trim() : "NASAR PK";

        String formattedDate = formatDate(request.getPaymentDate());
        String paymentType = request.getPaymentType() != null ? request.getPaymentType().trim().toUpperCase() : "ALL";

        List<BankPayRecordDto> allRecords = request.getRecords() != null ? request.getRecords() : Collections.emptyList();
        List<BankPayRecordDto> targetRecords = new ArrayList<>();

        String mode = "NEFT";
        if ("SAME_BANK".equalsIgnoreCase(paymentType)) {
            mode = "ST"; // SBI Same Bank Transfer mode
            for (BankPayRecordDto r : allRecords) {
                if ("READY".equalsIgnoreCase(r.getStatus()) && "SAME_BANK".equalsIgnoreCase(r.getBankCategory())) {
                    targetRecords.add(r);
                }
            }
        } else if ("OTHER_BANK".equalsIgnoreCase(paymentType)) {
            mode = "NEFT"; // SBI Other Bank Interbank mode
            for (BankPayRecordDto r : allRecords) {
                if ("READY".equalsIgnoreCase(r.getStatus()) && "OTHER_BANK".equalsIgnoreCase(r.getBankCategory())) {
                    targetRecords.add(r);
                }
            }
        } else {
            // ALL Ready records
            for (BankPayRecordDto r : allRecords) {
                if ("READY".equalsIgnoreCase(r.getStatus())) {
                    targetRecords.add(r);
                }
            }
        }

        // Calculate Total Debit Amount for selected category
        BigDecimal totalDebit = targetRecords.stream()
                .map(BankPayRecordDto::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Header Record
        // Format: <Company Account>#<Branch Code>#<Payment Date>#<Total Debit>###<Company Name>#<MODE>
        txt.append(companyAccount).append("#");
        txt.append(branchCode).append("#");
        txt.append(formattedDate).append("#");
        txt.append(totalDebit.longValue()).append("###");
        txt.append(companyName).append("#").append(mode).append("\n");

        // 2. Employee Records
        // Format: <Bank Account>#<IFSC Code>#<Payment Date>##<Amount>##<Company Name>#<MODE>
        for (BankPayRecordDto rec : targetRecords) {
            String acct = rec.getAccountNumber() != null ? rec.getAccountNumber().trim() : "";
            String ifsc = rec.getIfscCode() != null ? rec.getIfscCode().trim() : "";
            long amt = rec.getAmount() != null ? rec.getAmount().longValue() : 0;

            // Individual record mode
            String recMode = "SAME_BANK".equalsIgnoreCase(rec.getBankCategory()) ? "ST" : "NEFT";
            if (!"SAME_BANK".equalsIgnoreCase(paymentType) && !"OTHER_BANK".equalsIgnoreCase(paymentType)) {
                recMode = mode;
            }

            txt.append(acct).append("#");
            txt.append(ifsc).append("#");
            txt.append(formattedDate).append("##");
            txt.append(amt).append("##");
            txt.append(companyName).append("#").append(recMode).append("\n");
        }

        return txt.toString();
    }

    public byte[] generateExceptionReportExcel(List<BankPayRecordDto> records) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Exception Report");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee Name", "Amount (INR)", "Status", "Failure Reason"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            List<BankPayRecordDto> failedRecords = records != null ? records.stream()
                    .filter(r -> !"READY".equalsIgnoreCase(r.getStatus()))
                    .toList() : Collections.emptyList();

            for (BankPayRecordDto rec : failedRecords) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rec.getEmployeeName() != null ? rec.getEmployeeName() : "");
                row.createCell(1).setCellValue(rec.getAmount() != null ? rec.getAmount().doubleValue() : 0.0);
                row.createCell(2).setCellValue(rec.getStatus() != null ? rec.getStatus() : "FAILED");
                row.createCell(3).setCellValue(rec.getReason() != null ? rec.getReason() : "Validation Error");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Exception Report Excel: " + e.getMessage(), e);
        }
    }

    private String formatDate(String dateInput) {
        if (dateInput == null || dateInput.isBlank()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        try {
            if (dateInput.contains("-")) {
                LocalDate parsed = LocalDate.parse(dateInput.trim());
                return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (dateInput.contains("/")) {
                return dateInput.trim();
            }
        } catch (Exception ignored) {}
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    double dVal = cell.getNumericCellValue();
                    if (dVal == (long) dVal) {
                        return String.valueOf((long) dVal);
                    } else {
                        return String.valueOf(dVal);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private BigDecimal parseAmount(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    if (val.isEmpty()) return BigDecimal.ZERO;
                    return new BigDecimal(val.replaceAll(",", ""));
                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        return BigDecimal.ZERO;
                    }
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
