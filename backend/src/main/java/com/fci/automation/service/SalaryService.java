package com.fci.automation.service;

import com.fci.automation.entity.DailyBagsWork;
import com.fci.automation.entity.DailyClauseWork;
import com.fci.automation.entity.DailyHeadcount;
import com.fci.automation.entity.PayrollEntry;
import com.fci.automation.entity.PayrollPeriod;
import com.fci.automation.repository.DailyBagsWorkRepository;
import com.fci.automation.repository.DailyClauseWorkRepository;
import com.fci.automation.repository.DailyHeadcountRepository;
import com.fci.automation.repository.PayrollEntryRepository;
import com.fci.automation.repository.PayrollPeriodRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class SalaryService {

    private final DailyBagsWorkRepository bagsRepo;
    private final DailyClauseWorkRepository clauseRepo;
    private final PayrollPeriodRepository periodRepo;
    private final PayrollEntryRepository entryRepo;
    private final DailyHeadcountRepository headcountRepo;

    // Rates according to user Excel rules
    private static final BigDecimal RATE_UPTO_10 = new BigDecimal("7.98");
    private static final BigDecimal RATE_11_16 = new BigDecimal("9.37");
    private static final BigDecimal RATE_17_20 = new BigDecimal("10.83");
    private static final BigDecimal RATE_ABOVE_20 = new BigDecimal("14.16");
    private static final BigDecimal RATE_CLAUSE_ISSUE = new BigDecimal("9.65");

    public SalaryService(DailyBagsWorkRepository bagsRepo, DailyClauseWorkRepository clauseRepo,
            PayrollPeriodRepository periodRepo, PayrollEntryRepository entryRepo,
            DailyHeadcountRepository headcountRepo) {
        this.bagsRepo = bagsRepo;
        this.clauseRepo = clauseRepo;
        this.periodRepo = periodRepo;
        this.entryRepo = entryRepo;
        this.headcountRepo = headcountRepo;
    }

    public DailyHeadcount saveHeadcount(DailyHeadcount headcount) {
        Optional<DailyHeadcount> existing = headcountRepo.findByDate(headcount.getDate());
        if (existing.isPresent()) {
            DailyHeadcount e = existing.get();
            e.setHeadcount(headcount.getHeadcount());
            return headcountRepo.save(e);
        }
        return headcountRepo.save(headcount);
    }

    public DailyBagsWork saveBagsWork(DailyBagsWork work) {
        Optional<DailyBagsWork> existing = bagsRepo.findByDate(work.getDate());
        if (existing.isPresent()) {
            DailyBagsWork e = existing.get();
            e.setWorkSlipNo(work.getWorkSlipNo());
            e.setBagsUpto10(work.getBagsUpto10());
            e.setBags11to16(work.getBags11to16());
            e.setBags17to20(work.getBags17to20());
            e.setBagsAbove20(work.getBagsAbove20());
            return bagsRepo.save(e);
        }
        return bagsRepo.save(work);
    }

    public DailyClauseWork saveClauseWork(DailyClauseWork work) {
        Optional<DailyClauseWork> existing = clauseRepo.findByDate(work.getDate());
        if (existing.isPresent()) {
            DailyClauseWork e = existing.get();
            e.setWorkSlipNo(work.getWorkSlipNo());
            e.setBagsClause15(work.getBagsClause15());
            return clauseRepo.save(e);
        }
        return clauseRepo.save(work);
    }

    public SalaryCalculationResult calculateDailySalary(LocalDate date) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Calculate Bags Total (Table 1)
        Optional<DailyBagsWork> bagsOpt = bagsRepo.findByDate(date);
        if (bagsOpt.isPresent()) {
            DailyBagsWork b = bagsOpt.get();
            totalAmount = totalAmount.add(new BigDecimal(b.getBagsUpto10()).multiply(RATE_UPTO_10))
                    .add(new BigDecimal(b.getBags11to16()).multiply(RATE_11_16))
                    .add(new BigDecimal(b.getBags17to20()).multiply(RATE_17_20))
                    .add(new BigDecimal(b.getBagsAbove20()).multiply(RATE_ABOVE_20));
        }

        // 2. Calculate Clause Total (Table 4)
        Optional<DailyClauseWork> clauseOpt = clauseRepo.findByDate(date);
        if (clauseOpt.isPresent()) {
            totalAmount = totalAmount
                    .add(new BigDecimal(clauseOpt.get().getBagsClause15()).multiply(RATE_CLAUSE_ISSUE));
        }

        // 3. Count Attendance (Check manual override first)
        Optional<DailyHeadcount> headcountOpt = headcountRepo.findByDate(date);
        int attendanceCount = headcountOpt.map(DailyHeadcount::getHeadcount)
                                          .orElseGet(() -> getAttendanceCountForDate(date));

        // 4. Per Person Salary
        BigDecimal perPersonSalary = BigDecimal.ZERO;
        if (attendanceCount > 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            perPersonSalary = totalAmount.divide(new BigDecimal(attendanceCount), 2, RoundingMode.HALF_UP);
        }

        return new SalaryCalculationResult(date, totalAmount, attendanceCount, perPersonSalary);
    }

    private int getAttendanceCountForDate(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        int day = date.getDayOfMonth();

        Optional<PayrollPeriod> periodOpt = periodRepo.findByMonthAndYear(ym.getMonthValue(), ym.getYear());
        if (periodOpt.isEmpty()) {
            return 0; // No payroll period exists, thus no attendance logged logically
        }

        List<PayrollEntry> entries = entryRepo.findByPeriodId(periodOpt.get().getId());
        long count = entries.stream()
                .filter(e -> e.getActiveDays() != null && e.getActiveDays().contains(day))
                .count();

        return (int) count;
    }

    public List<SalaryCalculationResult> calculateSalaryRange(LocalDate startDate, LocalDate endDate) {
        List<SalaryCalculationResult> results = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            SalaryCalculationResult result = calculateDailySalary(currentDate);
            if (result.totalAmount.compareTo(BigDecimal.ZERO) > 0 || result.totalAttendance > 0) {
                 results.add(result);
            }
            currentDate = currentDate.plusDays(1);
        }
        return results;
    }

    // Helper class for returning the calculation result
    public static class SalaryCalculationResult {
        public LocalDate date;
        public BigDecimal totalAmount;
        public int totalAttendance;
        public BigDecimal perPersonSalary;

        public SalaryCalculationResult(LocalDate date, BigDecimal totalAmount, int totalAttendance,
                BigDecimal perPersonSalary) {
            this.date = date;
            this.totalAmount = totalAmount;
            this.totalAttendance = totalAttendance;
            this.perPersonSalary = perPersonSalary;
        }
    }
}
