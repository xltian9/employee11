package com.limiti.employee.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度工资报告行
 *
 * <p>计算公式：</p>
 * <ul>
 *   <li>当月工作日：排除周末与内置节假日的自然工作日天数</li>
 *   <li>奖金：月薪 × 绩效系数（该季度绩效评分决定，见 SalaryReportService）</li>
 *   <li>实际工资：月薪 − 月薪 ÷ 工作日 × 请假天数 + 奖金</li>
 * </ul>
 *
 * @author Limiti
 */
@Data
public class SalaryReportDTO {

    /** 员工ID */
    private Long employeeId;

    /** 工号 */
    private String employeeNumber;

    /** 姓名 */
    private String name;

    /** 部门 */
    private String department;

    /** 岗位 */
    private String position;

    /** 年份 */
    private Integer yearNum;

    /** 月份 */
    private Integer monthNum;

    /** 当月工作日天数 */
    private Integer workDay;

    /** 月薪 */
    private BigDecimal wage;

    /** 当月请假天数（已通过的请假） */
    private Integer leavesDay;

    /** 该季度绩效评分，无记录则为 null */
    private Double performanceScore;

    /** 该季度绩效记录ID，无记录则为 null */
    private Long performanceId;

    /** 奖金 = 月薪 × 绩效系数 */
    private BigDecimal bonus;

    /** 实际工资 */
    private BigDecimal nowWage;
}
