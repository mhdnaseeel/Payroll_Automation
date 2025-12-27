package com.fci.automation.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // --- INPUTS ---
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "payroll_entry_days", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "active_day")
    private java.util.Set<Integer> activeDays = new java.util.HashSet<>();

    @Column(name = "days_worked")
    private Integer daysWorked = 0;

    @Column(name = "wages_earned", precision = 10, scale = 2)
    private BigDecimal wagesEarned = BigDecimal.ZERO;

    @Column(name = "advance_deduction", precision = 10, scale = 2)
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    // --- CALCULATED FIELDS (Read-Only Logic) ---

    // EPF - Employee (12%)
    @Column(name = "epf_member_share", precision = 10, scale = 2)
    private BigDecimal epfMemberShare = BigDecimal.ZERO;

    // EPF - Contractor (Employer)
    @Column(name = "epf_contractor_share", precision = 10, scale = 2)
    private BigDecimal epfContractorShare = BigDecimal.ZERO;

    // ESI - Employee (0.75%)
    @Column(name = "esi_member_share", precision = 10, scale = 2)
    private BigDecimal esiMemberShare = BigDecimal.ZERO;

    // ESI - Contractor (3.25%)
    @Column(name = "esi_contractor_share", precision = 10, scale = 2)
    private BigDecimal esiContractorShare = BigDecimal.ZERO;

    // Bonus Share (8.33%)
    @Column(name = "bonus_share", precision = 10, scale = 2)
    private BigDecimal bonusShare = BigDecimal.ZERO;

    // Net Pay
    @Column(name = "net_payable", precision = 10, scale = 2)
    private BigDecimal netPayable = BigDecimal.ZERO;

    // UTR Number (Imported from Bank)
    @Column(name = "utr_number")
    private String utrNumber;
}
