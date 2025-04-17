package com.limiti.employee.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 考勤记录实体类
 * @author Limiti
 * @date 2024/11/01
 */
@Data
@Entity
@Table(name = "attendances")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "note", columnDefinition = "text")
    private String note;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (checkInTime == null) {
            checkInTime = LocalDateTime.now();
        }
    }
} 