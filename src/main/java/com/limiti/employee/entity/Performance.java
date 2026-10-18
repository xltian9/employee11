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
@Table(name = "performances", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year", "quarter"}))
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT COMMENT '绩效ID'")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false, columnDefinition = "BIGINT COMMENT '员工ID'")
    private Employee employee;

    @Column(name = "year", nullable = false, columnDefinition = "INT COMMENT '年份'")
    private Integer year;

    @Column(name = "quarter", nullable = false, columnDefinition = "TINYINT COMMENT '季度：1-4'")
    private Integer quarter;

    @Column(name = "score", nullable = false, precision = 3, scale = 1, columnDefinition = "DECIMAL(3,1) COMMENT '评分'")
    private Double score;

    @Lob
    @Column(name = "evaluation", nullable = false, columnDefinition = "TEXT COMMENT '评价'")
    private String evaluation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evaluator_id", nullable = false, columnDefinition = "BIGINT COMMENT '评价人ID'")
    private Admin evaluator;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 0 COMMENT '状态：0-待确认，1-已确认'")
    private Integer status = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'")
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'")
    private Date updateTime;

    /** 历史字段：月度业绩已统一改为按季度记录，可为空 */
    @Column(name = "month")
    private Integer month;

    /** 历史字段：工作评分的旧字段，可为空 */
    @Column(name = "work_score")
    private Integer workScore;

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

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
} 