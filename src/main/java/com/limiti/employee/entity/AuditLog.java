package com.limiti.employee.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 审计日志。
 *
 * <p>记录「谁、什么时候、对谁、做了什么、改成什么」，用于事后追溯工资调整、请假审批、
 * 绩效录入等敏感操作。只追加不修改，因此没有 update_time。</p>
 *
 * @author Limiti
 */
@Data
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_time", columnList = "create_time"),
        @Index(name = "idx_audit_operator", columnList = "operator_name"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人ID（管理员或员工，匿名时为 null） */
    @Column(name = "operator_id")
    private Long operatorId;

    /** 操作人姓名（冗余存储，避免人员删除后查不到） */
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    /** 操作人类型：ADMIN / EMPLOYEE / SYSTEM */
    @Column(name = "operator_type", length = 20)
    private String operatorType;

    /** 动作编码，如 SALARY_ADJUST、LEAVE_APPROVE */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** 动作中文名，便于直接展示与检索 */
    @Column(name = "action_name", length = 50)
    private String actionName;

    /** 对象类型：EMPLOYEE / LEAVE / PERFORMANCE / POLICY ... */
    @Column(name = "target_type", length = 30)
    private String targetType;

    /** 对象ID */
    @Column(name = "target_id")
    private Long targetId;

    /** 对象名称（员工姓名、请假单号等） */
    @Column(name = "target_name", length = 200)
    private String targetName;

    /** 明细：变更前后内容 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 来源 IP */
    @Column(name = "ip", length = 64)
    private String ip;

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
