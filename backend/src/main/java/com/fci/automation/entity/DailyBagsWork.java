package com.fci.automation.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_bags_work")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyBagsWork {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "work_slip_no")
    private String workSlipNo;

    @Column(name = "bags_upto_10")
    private Integer bagsUpto10 = 0;

    @Column(name = "bags_11_to_16")
    private Integer bags11to16 = 0;

    @Column(name = "bags_17_to_20")
    private Integer bags17to20 = 0;

    @Column(name = "bags_above_20")
    private Integer bagsAbove20 = 0;
}
