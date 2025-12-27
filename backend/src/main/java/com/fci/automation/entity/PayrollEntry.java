package com.fci.automation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_entries")
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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PayrollPeriod getPeriod() {
        return period;
    }

    public void setPeriod(PayrollPeriod period) {
        this.period = period;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getDaysWorked() {
        return daysWorked;
    }

    public void setDaysWorked(Integer daysWorked) {
        this.daysWorked = daysWorked;
    }

    public java.util.Set<Integer> getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(java.util.Set<Integer> activeDays) {
        this.activeDays = activeDays;
    }

    public BigDecimal getWagesEarned() {
        return wagesEarned;
    }

    public void setWagesEarned(BigDecimal wagesEarned) {
        this.wagesEarned = wagesEarned;
    }

    public BigDecimal getAdvanceDeduction() {
        return advanceDeduction;
    }

    public void setAdvanceDeduction(BigDecimal advanceDeduction) {
        this.advanceDeduction = advanceDeduction;
    }

    public BigDecimal getEpfMemberShare() {
        return epfMemberShare;
    }

    public void setEpfMemberShare(BigDecimal epfMemberShare) {
        this.epfMemberShare = epfMemberShare;
    }

    public BigDecimal getEpfContractorShare() {
        return epfContractorShare;
    }

    public void setEpfContractorShare(BigDecimal epfContractorShare) {
        this.epfContractorShare = epfContractorShare;
    }

    public BigDecimal getEsiMemberShare() {
        return esiMemberShare;
    }

    public void setEsiMemberShare(BigDecimal esiMemberShare) {
        this.esiMemberShare = esiMemberShare;
    }

    public BigDecimal getEsiContractorShare() {
        return esiContractorShare;
    }

    public void setEsiContractorShare(BigDecimal esiContractorShare) {
        this.esiContractorShare = esiContractorShare;
    }

    public BigDecimal getBonusShare() {
        return bonusShare;
    }

    public void setBonusShare(BigDecimal bonusShare) {
        this.bonusShare = bonusShare;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    // UTR Number (Imported from Bank)
    @Column(name = "utr_number")
    private String utrNumber;

    public String getUtrNumber() {
        return utrNumber;
    }

    public void setUtrNumber(String utrNumber) {
        this.utrNumber = utrNumber;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable;
    }
}
