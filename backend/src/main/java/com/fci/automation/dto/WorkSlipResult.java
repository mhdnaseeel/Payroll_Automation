package com.fci.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkSlipResult {

    private String status;

    @JsonProperty("document_type")
    private String documentType;

    private String section;

    private Header header;
    private Quantities quantities;

    @JsonProperty("qc_details")
    private QcDetails qcDetails;

    private Confidence confidence;

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Quantities getQuantities() {
        return quantities;
    }

    public void setQuantities(Quantities quantities) {
        this.quantities = quantities;
    }

    public QcDetails getQcDetails() {
        return qcDetails;
    }

    public void setQcDetails(QcDetails qcDetails) {
        this.qcDetails = qcDetails;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    // Inner Classes
    public static class Header {
        @JsonProperty("work_slip_no")
        private String workSlipNo;

        @JsonProperty("date_of_operation")
        private String dateOfOperation;

        @JsonProperty("depot_name")
        private String depotName;

        @JsonProperty("shed_or_remarks_text")
        private String shedOrRemarksText;

        public String getWorkSlipNo() {
            return workSlipNo;
        }

        public void setWorkSlipNo(String workSlipNo) {
            this.workSlipNo = workSlipNo;
        }

        public String getDateOfOperation() {
            return dateOfOperation;
        }

        public void setDateOfOperation(String dateOfOperation) {
            this.dateOfOperation = dateOfOperation;
        }

        public String getDepotName() {
            return depotName;
        }

        public void setDepotName(String depotName) {
            this.depotName = depotName;
        }

        public String getShedOrRemarksText() {
            return shedOrRemarksText;
        }

        public void setShedOrRemarksText(String shedOrRemarksText) {
            this.shedOrRemarksText = shedOrRemarksText;
        }
    }

    public static class Quantities {
        @JsonProperty("total_bags_written")
        private String totalBagsWritten;

        @JsonProperty("bags_up_to_10_high")
        private String bagsUpTo10High;

        @JsonProperty("bags_11_to_16_high")
        private String bags11To16High;

        @JsonProperty("bags_17_to_20_high")
        private String bags17To20High;

        @JsonProperty("bags_above_20_high")
        private String bagsAbove20High;

        public String getTotalBagsWritten() {
            return totalBagsWritten;
        }

        public void setTotalBagsWritten(String totalBagsWritten) {
            this.totalBagsWritten = totalBagsWritten;
        }

        public String getBagsUpTo10High() {
            return bagsUpTo10High;
        }

        public void setBagsUpTo10High(String bagsUpTo10High) {
            this.bagsUpTo10High = bagsUpTo10High;
        }

        public String getBags11To16High() {
            return bags11To16High;
        }

        public void setBags11To16High(String bags11To16High) {
            this.bags11To16High = bags11To16High;
        }

        public String getBags17To20High() {
            return bags17To20High;
        }

        public void setBags17To20High(String bags17To20High) {
            this.bags17To20High = bags17To20High;
        }

        public String getBagsAbove20High() {
            return bagsAbove20High;
        }

        public void setBagsAbove20High(String bagsAbove20High) {
            this.bagsAbove20High = bagsAbove20High;
        }
    }

    public static class QcDetails {
        @JsonProperty("labour_count")
        private String labourCount;

        public String getLabourCount() {
            return labourCount;
        }

        public void setLabourCount(String labourCount) {
            this.labourCount = labourCount;
        }
    }

    public static class Confidence {
        @JsonProperty("overall")
        private String overall;

        public String getOverall() {
            return overall;
        }

        public void setOverall(String overall) {
            this.overall = overall;
        }
    }
}
