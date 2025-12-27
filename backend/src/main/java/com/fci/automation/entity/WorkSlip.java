package com.fci.automation.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_slips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
