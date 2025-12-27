package com.fci.automation.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "member_id", nullable = false, unique = true)
    private String memberId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "uan_number", unique = true)
    private String uanNumber;

    @Column(name = "ip_number", unique = true)
    private String ipNumber;

    @Column(name = "bank_account_no", unique = true)
    private String bankAccountNo;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "inactive_date")
    private java.time.LocalDate inactiveDate;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public enum Category {
        CL, HL
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;
}
