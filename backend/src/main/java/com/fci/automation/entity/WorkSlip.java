package com.fci.automation.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_slips")
public class WorkSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slip_number", unique = true, nullable = false)
    private String slipNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkSlipCategory category;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "image_path")
    private String imagePath; // Path to the uploaded original file (PDF/Image)

    // Common Fields
    @Column(name = "truck_number")
    private String truckNumber;

    // --- ISSUE FIELDS ---
    @Column(name = "issue_total_bags")
    private Integer issueTotalBags;

    // --- RECEIPT FIELDS (Height-wise Stacking) ---
    @Column(name = "receipt_total_bags")
    private Integer receiptTotalBags;

    @Column(name = "bags_upto_10")
    private Integer bagsUpTo10;

    @Column(name = "bags_11_to_16")
    private Integer bags11To16;

    @Column(name = "bags_17_to_20")
    private Integer bags17To20;

    @Column(name = "bags_above_20")
    private Integer bagsAbove20;

    // --- QC FIELDS (Casual Labour) ---
    @Column(name = "labour_count")
    private Integer labourCount;

    // Common Metadata
    @Column(name = "shed_details")
    private String shedDetails; // "Depot / Shed / Remarks"

    // Removed generic 'quantity' and 'commodity' as they are now specific
    // Removed 'laborCost' - calculation happens at bill generation, this is raw
    // data.

    public enum WorkSlipCategory {
        ISSUE, RECEIPT, QC
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlipNumber() {
        return slipNumber;
    }

    public void setSlipNumber(String slipNumber) {
        this.slipNumber = slipNumber;
    }

    public WorkSlipCategory getCategory() {
        return category;
    }

    public void setCategory(WorkSlipCategory category) {
        this.category = category;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getIssueTotalBags() {
        return issueTotalBags;
    }

    public void setIssueTotalBags(Integer issueTotalBags) {
        this.issueTotalBags = issueTotalBags;
    }

    public Integer getReceiptTotalBags() {
        return receiptTotalBags;
    }

    public void setReceiptTotalBags(Integer receiptTotalBags) {
        this.receiptTotalBags = receiptTotalBags;
    }

    public Integer getBagsUpTo10() {
        return bagsUpTo10;
    }

    public void setBagsUpTo10(Integer bagsUpTo10) {
        this.bagsUpTo10 = bagsUpTo10;
    }

    public Integer getBags11To16() {
        return bags11To16;
    }

    public void setBags11To16(Integer bags11To16) {
        this.bags11To16 = bags11To16;
    }

    public Integer getBags17To20() {
        return bags17To20;
    }

    public void setBags17To20(Integer bags17To20) {
        this.bags17To20 = bags17To20;
    }

    public Integer getBagsAbove20() {
        return bagsAbove20;
    }

    public void setBagsAbove20(Integer bagsAbove20) {
        this.bagsAbove20 = bagsAbove20;
    }

    public Integer getLabourCount() {
        return labourCount;
    }

    public void setLabourCount(Integer labourCount) {
        this.labourCount = labourCount;
    }

    public String getShedDetails() {
        return shedDetails;
    }

    public void setShedDetails(String shedDetails) {
        this.shedDetails = shedDetails;
    }
}
