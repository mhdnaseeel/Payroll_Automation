package com.fci.automation.dto;

import java.util.ArrayList;
import java.util.List;

public class EmployeeImportSummary {
    private int totalRecords;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private int failedCount;
    private List<ImportFailureDetail> failedRecords = new ArrayList<>();
    private List<ImportUpdateDetail> updatedRecords = new ArrayList<>();

    public EmployeeImportSummary() {}

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }

    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public List<ImportFailureDetail> getFailedRecords() { return failedRecords; }
    public void setFailedRecords(List<ImportFailureDetail> failedRecords) { this.failedRecords = failedRecords; }

    public List<ImportUpdateDetail> getUpdatedRecords() { return updatedRecords; }
    public void setUpdatedRecords(List<ImportUpdateDetail> updatedRecords) { this.updatedRecords = updatedRecords; }

    // --- Failure Detail ---
    public static class ImportFailureDetail {
        private int rowNumber;
        private String memberId;
        private String name;
        private String reason;

        public ImportFailureDetail() {}

        public ImportFailureDetail(int rowNumber, String memberId, String name, String reason) {
            this.rowNumber = rowNumber;
            this.memberId = memberId;
            this.name = name;
            this.reason = reason;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // --- Update Detail ---
    public static class ImportUpdateDetail {
        private int rowNumber;
        private String memberId;
        private String name;
        private List<FieldChange> changes;

        public ImportUpdateDetail() {}

        public ImportUpdateDetail(int rowNumber, String memberId, String name, List<FieldChange> changes) {
            this.rowNumber = rowNumber;
            this.memberId = memberId;
            this.name = name;
            this.changes = changes;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<FieldChange> getChanges() { return changes; }
        public void setChanges(List<FieldChange> changes) { this.changes = changes; }
    }

    // --- Field-level change (old → new) ---
    public static class FieldChange {
        private String field;
        private String oldValue;
        private String newValue;

        public FieldChange() {}

        public FieldChange(String field, String oldValue, String newValue) {
            this.field = field;
            this.oldValue = oldValue != null ? oldValue : "-";
            this.newValue = newValue != null ? newValue : "-";
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }
}
