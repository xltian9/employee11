package com.limiti.employee.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 工资调整单：补发与扣款。
 *
 * <p>工资报告会自动汇总当月所有调整单，因此人工调整不需要改工资基数，
 * 每一笔都能追溯到录入人和录入时间。</p>
 *
 * @author Limiti
 */
@Data
@Entity
@Table(name = "salary_adjustments", indexes = {
        @Index(name = "idx_adjust_employee_period", columnList = "employee_id,year_num,month_num")
})
public class SalaryAdjustment {

    /** 补发 */
    public static final int TYPE_ADD = 1;

    /** 扣款 */
    public static final int TYPE_DEDUCT = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /** 员工姓名快照 */
    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "year_num", nullable = false)
    private Integer year;

    @Column(name = "month_num", nullable = false)
    private Integer month;

    /** 1-补发，2-扣款 */
    @Column(name = "type", nullable = false)
    private Integer type;

    /** 金额，始终为正数 */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

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
