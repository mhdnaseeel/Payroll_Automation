package com.fci.automation;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExcelHeaderInspector {

    @Test
    public void printEsiHeaders() throws Exception {
        Path path = Paths.get("/Users/muhammednaseel/Desktop/Project/hr automation/ESI.xls");
        System.out.println("Reading file: " + path);

        try (FileInputStream fis = new FileInputStream(path.toFile());
                Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            StringBuilder sb = new StringBuilder();
            sb.append("Sheet Name: ").append(sheet.getSheetName()).append("\n");

            // Iterate first few rows to find header
            for (int i = 0; i < 5; i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                sb.append("Row ").append(i).append(": ");
                for (Cell cell : row) {
                    sb.append("[").append(cell.getColumnIndex()).append("]: ").append(cell.toString()).append(" | ");
                }
                sb.append("\n");
            }
            throw new RuntimeException("INSPECTION OUTPUT:\n" + sb.toString());
        }
    }
}
