package com.limiti.employee.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * 请假实体类
 * @author Limiti
 * @date 2024/11/15
 */
@Data
@Entity
@Table(name = "leaves")
public class Leave {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer type;  // 1-事假，2-病假，3-年假，4-调休

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_time", nullable = false)
    private Date endTime;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private Integer status = 0;  // 0-待审核，1-已通过，2-已拒绝

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approver_id")
    private Admin approver;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approve_time")
    private Date approveTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @Column(name = "leaves_day")
    private Integer leavesDay;  //请假天数

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
} 