package com.limiti.employee.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 加班记录。
 *
 * <p>由管理员录入（或从考勤备注补录），工资报告按类型套用不同费率：
 * 1-工作日 1.5 倍、2-周末 2 倍、3-法定节假日 3 倍，倍数可在薪酬规则里调整。</p>
 *
 * @author Limiti
 */
@Data
@Entity
@Table(name = "overtime_records", indexes = {
        @Index(name = "idx_overtime_employee_date", columnList = "employee_id,work_date")
})
public class OvertimeRecord {

    /** 工作日加班 */
    public static final int TYPE_WEEKDAY = 1;

    /** 周末加班 */
    public static final int TYPE_WEEKEND = 2;

    /** 法定节假日加班 */
    public static final int TYPE_HOLIDAY = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** 加班小时数 */
    @Column(name = "hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    /** 1-工作日，2-周末，3-法定节假日 */
    @Column(name = "ot_type", nullable = false)
    private Integer otType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = new Date();
        }
    }
}
