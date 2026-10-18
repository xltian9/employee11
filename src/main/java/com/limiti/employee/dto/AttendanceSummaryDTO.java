package com.limiti.employee.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度考勤汇总。
 *
 * <p>由 {@code AttendanceStatsService} 按"当月工作日逐天比对考勤记录 + 已批准请假"实时计算，
 * 不需要定时任务预生成，因此请假审批、撤销后立即生效。</p>
 *
 * @author Limiti
 */
@Data
public class AttendanceSummaryDTO {

    private Long employeeId;
    private String employeeNumber;
    private String name;
    private Integer yearNum;
    private Integer monthNum;

    /** 当月应出勤工作日 */
    private Integer workDays = 0;

    /** 实际有打卡记录的天数 */
    private Integer actualDays = 0;

    /** 正常出勤天数 */
    private Integer normalDays = 0;

    /** 迟到次数 */
    private Integer lateCount = 0;

    /** 早退次数 */
    private Integer earlyCount = 0;

    /** 旷工天数 */
    private Integer absentDays = 0;

    /** 请假天数（已批准，只统计工作日） */
    private Integer leaveDays = 0;

    /** 事假天数 */
    private Integer casualLeaveDays = 0;

    /** 病假天数 */
    private Integer sickLeaveDays = 0;

    /** 年假 + 调休天数 */
    private Integer otherLeaveDays = 0;

    private BigDecimal overtimeWeekdayHours = BigDecimal.ZERO;
    private BigDecimal overtimeWeekendHours = BigDecimal.ZERO;
    private BigDecimal overtimeHolidayHours = BigDecimal.ZERO;

    /** 加班总小时数 */
    private BigDecimal overtimeHours = BigDecimal.ZERO;
}
