package com.fci.automation.service;

import com.fci.automation.entity.Employee;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
public class PayrollImportService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayrollEntryRepository entryRepository;

    @Autowired
    private PayrollPeriodRepository periodRepository;

    @Autowired
    private PayrollCalculatorService calculatorService;

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll Import");
            Row header = sheet.createRow(0);

            String[] headers = { "MEMBER_ID", "MEMBER_NAME", "DAYS_WORKED", "WAGES_EARNED", "ADVANCE" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    @Transactional
    public String importPayroll(MultipartFile file, UUID periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip Header
            if (rowIterator.hasNext())
                rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    processRow(row, period);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        StringBuilder result = new StringBuilder();
        result.append("Import Completed. Success: ").append(successCount).append(", Failed: ").append(failCount);
        if (!errors.isEmpty()) {
            result.append("\nErrors:\n").append(String.join("\n", errors));
        }
        return result.toString();
    }

    private void processRow(Row row, PayrollPeriod period) {
        // Col 0: MEMBER_ID
        Cell memberIdCell = row.getCell(0);
        if (memberIdCell == null)
            return;

        String memberId = getCellValueAsString(memberIdCell);
        if (memberId.trim().isEmpty())
            return;

        Employee employee = employeeRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Employee not found with Member ID: " + memberId));

        // Find or Create Entry
        PayrollEntry entry = entryRepository.findByPeriodIdAndEmployeeId(period.getId(), employee.getId())
                .orElse(new PayrollEntry());

        if (entry.getId() == null) {
            entry.setPeriod(period);
            entry.setEmployee(employee);
        }

        // Col 2: DAYS_WORKED
        // Always overwrite. If null/empty -> 0
        int daysWorked = getCellValueAsInteger(row.getCell(2));
        entry.setDaysWorked(daysWorked);

        // Col 3: WAGES_EARNED
        // Always overwrite. If null/empty -> 0.00
        BigDecimal wages = getCellValueAsBigDecimal(row.getCell(3));
        entry.setWagesEarned(wages);

        // Col 4: ADVANCE
        // Always overwrite. If null/empty -> 0.00
        BigDecimal advance = getCellValueAsBigDecimal(row.getCell(4));
        System.out.println("DEBUG IMPORT: Row " + row.getRowNum() + " | Member: " + memberId + " | Advance Cell: "
                + row.getCell(4) + " | Parsed Advance: " + advance);
        entry.setAdvanceDeduction(advance);

        // Trigger Calculation
        calculatorService.calculate(entry);

        entryRepository.save(entry);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                case FORMULA -> {
                    try {
                        yield String.valueOf((int) cell.getNumericCellValue());
                    } catch (Exception e) {
                        try {
                            yield cell.getStringCellValue();
                        } catch (Exception ex) {
                            yield "";
                        }
                    }
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    if (val.isEmpty())
                        return BigDecimal.ZERO;
                    try {
                        return new BigDecimal(val);
                    } catch (NumberFormatException e) {
                        // Handle "1,500" or similar if needed, commonly simple replace
                        val = val.replace(",", "");
                        try {
                            return new BigDecimal(val);
                        } catch (NumberFormatException ex) {
                            return BigDecimal.ZERO; // Not a number
                        }
                    }
                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        return BigDecimal.ZERO;
                    }
                case BLANK:
                    return BigDecimal.ZERO;
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return 0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    if (val.isEmpty())
                        return 0;
                    try {
                        return Integer.parseInt(val); // Strict int
                    } catch (NumberFormatException e) {
                        try {
                            return (int) Double.parseDouble(val); // Handle "5.0"
                        } catch (NumberFormatException ex) {
                            return 0;
                        }
                    }
                case FORMULA:
                    try {
                        return (int) cell.getNumericCellValue();
                    } catch (Exception e) {
                        return 0;
                    }
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public String importUtrData(MultipartFile file, UUID periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            // Valid Excel File
            Sheet sheet = workbook.getSheetAt(0);
            for (Row r : sheet) {
                List<String> rowData = new ArrayList<>();
                for (int cn = 0; cn < r.getLastCellNum(); cn++) {
                    rowData.add(getAnyCellValue(r.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)));
                }
                rows.add(rowData);
            }
        } catch (Exception e) {
            // Fallback: Try parsing as text (HTML Table, XML Spreadsheet, CSV)
            try {
                String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                rows = parseNonStandardExcel(content);
                if (rows.isEmpty()) {
                    throw new RuntimeException(
                            "Could not parse file. It does not appear to be a valid Excel, HTML Table, XML Spreadsheet, or CSV file. Original Error: "
                                    + e.getMessage());
                }
            } catch (Exception ex) {
                return "Import Failed: " + ex.getMessage();
            }
        }

        return processUtrRows(rows, period);
    }

    private List<List<String>> parseNonStandardExcel(String content) {
        List<List<String>> table = new ArrayList<>();

        // 1. Try XML Spreadsheet (SpreadsheetML) commonly used by banks
        // Pattern: <Row ...> <Cell ...> <Data ...>Value</Data> </Cell> </Row>
        if (content.contains("<Row") || content.contains("<Worksheet")) {
            java.util.regex.Pattern rowPat = java.util.regex.Pattern.compile("<Row[^>]*>(.*?)</Row>",
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern cellPat = java.util.regex.Pattern.compile("<Cell[^>]*>(.*?)</Cell>",
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern dataPat = java.util.regex.Pattern.compile("<Data[^>]*>(.*?)</Data>",
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);

            java.util.regex.Matcher rowM = rowPat.matcher(content);
            while (rowM.find()) {
                List<String> rowData = new ArrayList<>();
                java.util.regex.Matcher cellM = cellPat.matcher(rowM.group(1));
                while (cellM.find()) {
                    String cellContent = cellM.group(1);
                    java.util.regex.Matcher dataM = dataPat.matcher(cellContent);
                    if (dataM.find()) {
                        rowData.add(cleanTagBytes(dataM.group(1)));
                    } else {
                        rowData.add("");
                    }
                }
                if (!rowData.isEmpty())
                    table.add(rowData);
            }
            if (!table.isEmpty())
                return table;
        }

        // 2. Try HTML Table
        if (content.contains("<tr") || content.contains("<table")) {
            java.util.regex.Pattern trPattern = java.util.regex.Pattern.compile("<tr[^>]*>(.*?)</tr>",
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Pattern tdPattern = java.util.regex.Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>",
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);

            java.util.regex.Matcher trMatcher = trPattern.matcher(content);
            while (trMatcher.find()) {
                String rowContent = trMatcher.group(1);
                List<String> row = new ArrayList<>();
                java.util.regex.Matcher tdMatcher = tdPattern.matcher(rowContent);
                while (tdMatcher.find()) {
                    row.add(cleanTagBytes(tdMatcher.group(1)));
                }
                if (!row.isEmpty()) {
                    table.add(row);
                }
            }
            if (!table.isEmpty())
                return table;
        }

        // 3. Try CSV / Tab Delimited (last resort)
        // If content looks like lines
        String[] lines = content.split("\n");
        if (lines.length > 1) {
            for (String line : lines) {
                if (line.trim().isEmpty())
                    continue;
                String[] cols = line.split("\t"); // Try Tab first
                if (cols.length < 2)
                    cols = line.split(","); // Then Comma
                if (cols.length > 0) {
                    List<String> row = new ArrayList<>();
                    for (String c : cols) {
                        // Remove quotes if CSV
                        String val = c.trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        row.add(val);
                    }
                    table.add(row);
                }
            }
        }

        return table;
    }

    private String cleanTagBytes(String text) {
        if (text == null)
            return "";
        // Remove tags
        text = text.replaceAll("<[^>]+>", "").trim();
        // Unescape common HTML/XML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .replace("&quot;", "\"");
        return text;
    }

    private String processUtrRows(List<List<String>> rows, PayrollPeriod period) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        // 1. Find Header Headers
        int acctColIdx = -1;
        int utrColIdx = -1;
        int startRowIdx = -1;

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            boolean foundExplicitUtr = false;

            for (int j = 0; j < row.size(); j++) {
                String val = row.get(j).toLowerCase();
                if (val.contains("account") || val.contains("beneficiary")) {
                    acctColIdx = j;
                }

                // Specific check for "UTR Number" (High Priority)
                if (val.contains("utr number")) {
                    utrColIdx = j;
                    foundExplicitUtr = true;
                } else if (!foundExplicitUtr
                        && (val.contains("utr") || val.contains("ref") || val.contains("transaction"))) {
                    // Fallback to other keywords ONLY if explicit match wasn't found in this row
                    // yet
                    // Note: This logic might still be flawed if "UTR Number" appears AFTER "UTR
                    // Status" in the same row?
                    // No, usually headers are distinct.
                    // But if "UTR Status" (contains 'utr') appears BEFORE "UTR Number", this
                    // else-if will trigger first.
                    // So we must ensure if we find 'utr number' we override whatever we found
                    // before.
                    // But if we found 'utr number', we set flag.
                    // If we later find 'utr status', we check flag. If flag is true, we SKIP
                    // overwriting.

                    // The issue is order.
                    // If "UTR Generic" comes first, it sets utrColIdx.
                    // If "UTR Number" comes later, it SHOULD override.
                    // If "UTR Number" comes first, later "UTR Generic" SHOULD NOT override.

                    // So:
                    // 1. If matches "utr number", ALWAYS set utrColIdx and set foundExplicitUtr =
                    // true.
                    // 2. If matches generic "utr" etc., set utrColIdx ONLY IF !foundExplicitUtr.
                    utrColIdx = j;
                }
            }
            if (acctColIdx != -1 && utrColIdx != -1) {
                startRowIdx = i + 1;
                break;
            }
        }

        if (acctColIdx == -1 || utrColIdx == -1) {
            return "Failed: Could not identify 'Account' or 'UTR' columns.";
        }

        // 2. Process Data
        for (int i = startRowIdx; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            // Handle row length mismatch
            if (row.size() <= Math.max(acctColIdx, utrColIdx))
                continue;

            try {
                String acctNo = row.get(acctColIdx).trim();
                // Remove Excel text marker (') if present
                if (acctNo.startsWith("'")) {
                    acctNo = acctNo.substring(1);
                }

                String utrNo = row.get(utrColIdx).trim();
                if (utrNo.startsWith("'")) {
                    utrNo = utrNo.substring(1);
                }

                if (acctNo.isEmpty())
                    continue;

                var employeeOpt = employeeRepository.findByBankAccountNo(acctNo);
                if (employeeOpt.isEmpty()) {
                    continue; // Skip unknown accounts
                }

                Employee employee = employeeOpt.get();
                var entryOpt = entryRepository.findByPeriodIdAndEmployeeId(period.getId(), employee.getId());
                if (entryOpt.isPresent()) {
                    PayrollEntry entry = entryOpt.get();
                    entry.setUtrNumber(utrNo);
                    entryRepository.save(entry);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        return String.format("Import Complete: %d Updated. %s",
                successCount,
                errors.isEmpty() ? "" : " Errors: " + String.join(", ", errors));
    }

    private String getAnyCellValue(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue()); // Keep decimals just in case, logic handles .0
                                                                        // removal
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
