package com.limiti.employee.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体类
 *
 * <p>{@code date} 是打卡归属日期（表里 NOT NULL 且有 employee_id + date 唯一键），
 * {@code status} 是 0-正常、1-迟到、2-早退、3-旷工、4-迟到+早退 的整数编码。</p>
 *
 * @author Limiti
 * @date 2024/11/01
 */
@Data
@Entity
@Table(name = "attendances")
public class Attendance {

    /** 正常 */
    public static final int STATUS_NORMAL = 0;
    /** 迟到 */
    public static final int STATUS_LATE = 1;
    /** 早退 */
    public static final int STATUS_EARLY = 2;
    /** 旷工 */
    public static final int STATUS_ABSENT = 3;
    /** 迟到 + 早退 */
    public static final int STATUS_LATE_AND_EARLY = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** 打卡归属日期（用于唯一约束与按天查询） */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    /** 0-正常，1-迟到，2-早退，3-旷工，4-迟到+早退 */
    @Column(name = "status", nullable = false)
    private Integer status = STATUS_NORMAL;

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
        if (date == null && checkInTime != null) {
            date = checkInTime.toLocalDate();
        }
    }

    /**
     * 状态中文名（展示用）
     */
    public static String statusName(Integer status) {
        if (status == null) {
            return "正常";
        }
        switch (status) {
            case STATUS_LATE:
                return "迟到";
            case STATUS_EARLY:
                return "早退";
            case STATUS_ABSENT:
                return "旷工";
            case STATUS_LATE_AND_EARLY:
                return "迟到+早退";
            default:
                return "正常";
        }
    }
}
