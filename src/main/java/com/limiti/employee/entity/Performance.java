package com.limiti.employee.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * 业绩实体类
 * @author Limiti
 * @date 2024/11/15
 */
@Data
@Entity
@Table(name = "performances")
public class Performance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "work_score", nullable = false)
    private Integer workScore;

    @Column(name = "attitude_score", nullable = false)
    private Integer attitudeScore;

    @Column(name = "attendance_score", nullable = false)
    private Integer attendanceScore;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Admin evaluator;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
        // 计算总分
        totalScore = (workScore + attitudeScore + attendanceScore) / 3;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
        // 更新总分
        totalScore = (workScore + attitudeScore + attendanceScore) / 3;
    }
} 