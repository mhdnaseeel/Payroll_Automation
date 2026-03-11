package com.fci.automation.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_clause_work")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyClauseWork {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "work_slip_no")
    private String workSlipNo;

    @Column(name = "bags_clause_15")
    private Integer bagsClause15 = 0;
}
