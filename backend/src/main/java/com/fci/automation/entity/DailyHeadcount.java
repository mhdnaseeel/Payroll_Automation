package com.fci.automation.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_headcount", uniqueConstraints = {
        @UniqueConstraint(columnNames = "date")
})
public class DailyHeadcount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false)
    private Integer headcount;

    public DailyHeadcount() {
    }

    public DailyHeadcount(LocalDate date, Integer headcount) {
        this.date = date;
        this.headcount = headcount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getHeadcount() {
        return headcount;
    }

    public void setHeadcount(Integer headcount) {
        this.headcount = headcount;
    }
}
