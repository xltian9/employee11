package com.limiti.employee.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪酬规则配置（全系统只有一行，id 固定为 1）。
 *
 * <p>把「请假怎么扣、迟到怎么扣、社保公积金比例、个税起征点、加班费率、绩效奖金系数」
 * 从代码里挪到配置里，管理员可以在「系统设置」页面直接改，改完立即生效。</p>
 *
 * @author Limiti
 */
@Data
@Entity
@Table(name = "salary_policy")
public class SalaryPolicy {

    /** 固定主键 */
    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** 请假扣款方式：0-按日工资，1-每天固定金额 */
    @Column(name = "leave_deduct_mode", nullable = false)
    private Integer leaveDeductMode = 0;

    /** 固定金额模式下的每日扣款金额 */
    @Column(name = "leave_deduct_amount", precision = 10, scale = 2)
    private BigDecimal leaveDeductAmount = new BigDecimal("200.00");

    /** 病假扣款比例（相对事假），0.5 表示病假只扣一半 */
    @Column(name = "sick_leave_rate", precision = 4, scale = 2)
    private BigDecimal sickLeaveRate = new BigDecimal("0.50");

    /** 旷工一天折算几天工资 */
    @Column(name = "absent_deduct_days", precision = 4, scale = 2)
    private BigDecimal absentDeductDays = new BigDecimal("3.00");

    /** 迟到/早退每次扣款金额 */
    @Column(name = "late_deduct_amount", precision = 10, scale = 2)
    private BigDecimal lateDeductAmount = new BigDecimal("50.00");

    /** 工作日加班费倍数 */
    @Column(name = "overtime_weekday_rate", precision = 4, scale = 2)
    private BigDecimal overtimeWeekdayRate = new BigDecimal("1.50");

    /** 周末加班费倍数 */
    @Column(name = "overtime_weekend_rate", precision = 4, scale = 2)
    private BigDecimal overtimeWeekendRate = new BigDecimal("2.00");

    /** 法定节假日加班费倍数 */
    @Column(name = "overtime_holiday_rate", precision = 4, scale = 2)
    private BigDecimal overtimeHolidayRate = new BigDecimal("3.00");

    /** 社保个人缴纳比例（养老8% + 医疗2% + 失业0.5%） */
    @Column(name = "social_insurance_rate", precision = 5, scale = 4)
    private BigDecimal socialInsuranceRate = new BigDecimal("0.1050");

    /** 住房公积金个人缴纳比例 */
    @Column(name = "housing_fund_rate", precision = 5, scale = 4)
    private BigDecimal housingFundRate = new BigDecimal("0.0700");

    /** 个税起征点 */
    @Column(name = "tax_threshold", precision = 10, scale = 2)
    private BigDecimal taxThreshold = new BigDecimal("5000.00");

    /** 绩效等级 S 的季度奖金系数 */
    @Column(name = "bonus_rate_s", precision = 5, scale = 4)
    private BigDecimal bonusRateS = new BigDecimal("0.2500");

    /** 绩效等级 A */
    @Column(name = "bonus_rate_a", precision = 5, scale = 4)
    private BigDecimal bonusRateA = new BigDecimal("0.1800");

    /** 绩效等级 B */
    @Column(name = "bonus_rate_b", precision = 5, scale = 4)
    private BigDecimal bonusRateB = new BigDecimal("0.1200");

    /** 绩效等级 C */
    @Column(name = "bonus_rate_c", precision = 5, scale = 4)
    private BigDecimal bonusRateC = new BigDecimal("0.0600");

    /** 绩效等级 D */
    @Column(name = "bonus_rate_d", precision = 5, scale = 4)
    private BigDecimal bonusRateD = new BigDecimal("0.0000");

    /** 上班时间（迟到判定） */
    @Column(name = "work_start", length = 5)
    private String workStart = "09:00";

    /** 下班时间（早退判定） */
    @Column(name = "work_end", length = 5)
    private String workEnd = "18:00";

    /** 迟到超过多少分钟算旷工（0 表示不启用） */
    @Column(name = "late_to_absent_minutes")
    private Integer lateToAbsentMinutes = 120;

    @Column(name = "update_by", length = 100)
    private String updateBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time")
    private Date updateTime;
}
