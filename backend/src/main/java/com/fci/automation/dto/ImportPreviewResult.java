package com.fci.automation.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportPreviewResult {
    private int totalRows;
    private List<NewEmployeePreview> toCreate = new ArrayList<>();
    private List<UpdateEmployeePreview> toUpdate = new ArrayList<>();
    private int skippedCount;
    private List<EmployeeImportSummary.ImportFailureDetail> failed = new ArrayList<>();

    public ImportPreviewResult() {}

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public List<NewEmployeePreview> getToCreate() { return toCreate; }
    public void setToCreate(List<NewEmployeePreview> toCreate) { this.toCreate = toCreate; }

    public List<UpdateEmployeePreview> getToUpdate() { return toUpdate; }
    public void setToUpdate(List<UpdateEmployeePreview> toUpdate) { this.toUpdate = toUpdate; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public List<EmployeeImportSummary.ImportFailureDetail> getFailed() { return failed; }
    public void setFailed(List<EmployeeImportSummary.ImportFailureDetail> failed) { this.failed = failed; }

    // --- New Employee Preview ---
    public static class NewEmployeePreview {
        private int rowNumber;
        private String memberId;
        private String fullName;
        private String uanNumber;
        private String ipNumber;
        private String bankAccountNo;
        private String ifscCode;
        private String category;

        public NewEmployeePreview() {}

        public NewEmployeePreview(int rowNumber, String memberId, String fullName,
                                  String uanNumber, String ipNumber, String bankAccountNo,
                                  String ifscCode, String category) {
            this.rowNumber = rowNumber;
            this.memberId = memberId;
            this.fullName = fullName;
            this.uanNumber = uanNumber;
            this.ipNumber = ipNumber;
            this.bankAccountNo = bankAccountNo;
            this.ifscCode = ifscCode;
            this.category = category;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getUanNumber() { return uanNumber; }
        public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }
        public String getIpNumber() { return ipNumber; }
        public void setIpNumber(String ipNumber) { this.ipNumber = ipNumber; }
        public String getBankAccountNo() { return bankAccountNo; }
        public void setBankAccountNo(String bankAccountNo) { this.bankAccountNo = bankAccountNo; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    // --- Update Employee Preview ---
    public static class UpdateEmployeePreview {
        private int rowNumber;
        private String memberId;
        private String fullName;
        private List<EmployeeImportSummary.FieldChange> changes;

        public UpdateEmployeePreview() {}

        public UpdateEmployeePreview(int rowNumber, String memberId, String fullName,
                                     List<EmployeeImportSummary.FieldChange> changes) {
            this.rowNumber = rowNumber;
            this.memberId = memberId;
            this.fullName = fullName;
            this.changes = changes;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public List<EmployeeImportSummary.FieldChange> getChanges() { return changes; }
        public void setChanges(List<EmployeeImportSummary.FieldChange> changes) { this.changes = changes; }
    }
}
