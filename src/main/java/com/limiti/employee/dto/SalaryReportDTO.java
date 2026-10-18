package com.limiti.employee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度工资明细。
 *
 * <p>工资构成（计算顺序见 {@code SalaryReportService}）：</p>
 * <pre>
 * 应发 = 月薪 − 请假扣款 − 旷工扣款 − 迟到早退扣款 + 加班费 + 季度奖金 + 补发 − 扣款
 * 社保 = 月薪 × 社保比例
 * 公积金 = 月薪 × 公积金比例
 * 个税 = (应发 − 社保 − 公积金 − 起征点) 按月度超额累进税率
 * 实发 = 应发 − 社保 − 公积金 − 个税
 * </pre>
 *
 * @author Limiti
 */
@Data
public class SalaryReportDTO {

    private Long employeeId;
    private String employeeNumber;
    private String name;
    private String department;
    private String position;
    private Integer yearNum;
    private Integer monthNum;

    /** 当月应出勤工作日 */
    private Integer workDay;

    /** 月薪 */
    private BigDecimal wage = BigDecimal.ZERO;

    // ---------- 考勤 ----------
    private Integer leaveDays = 0;
    private Integer casualLeaveDays = 0;
    private Integer sickLeaveDays = 0;
    private Integer otherLeaveDays = 0;
    private Integer lateCount = 0;
    private Integer earlyCount = 0;
    private Integer absentDays = 0;
    /** 实际出勤天数（有打卡记录的工作日） */
    private Integer actualDays = 0;

    // ---------- 加班 ----------
    private BigDecimal overtimeWeekdayHours = BigDecimal.ZERO;
    private BigDecimal overtimeWeekendHours = BigDecimal.ZERO;
    private BigDecimal overtimeHolidayHours = BigDecimal.ZERO;
    private BigDecimal overtimePay = BigDecimal.ZERO;

    // ---------- 扣款 ----------
    private BigDecimal leaveDeduct = BigDecimal.ZERO;
    private BigDecimal absentDeduct = BigDecimal.ZERO;
    private BigDecimal lateDeduct = BigDecimal.ZERO;

    // ---------- 绩效 ----------
    private Double performanceScore;
    private String performanceGrade;
    private Long performanceId;
    private BigDecimal bonus = BigDecimal.ZERO;

    // ---------- 调整 ----------
    private BigDecimal adjustAdd = BigDecimal.ZERO;
    private BigDecimal adjustDeduct = BigDecimal.ZERO;

    // ---------- 社保、公积金、个税 ----------
    private BigDecimal socialInsurance = BigDecimal.ZERO;
    private BigDecimal housingFund = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;

    /** 应发合计 */
    private BigDecimal grossPay = BigDecimal.ZERO;

    /** 实发合计 */
    private BigDecimal netPay = BigDecimal.ZERO;

    /** 兼容旧前端字段，等同于 netPay */
    private BigDecimal nowWage = BigDecimal.ZERO;

    private List<SalaryAdjustmentDTO> adjustments = new ArrayList<>();
}
