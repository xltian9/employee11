package com.limiti.employee.dto;

import lombok.Data;

import java.util.Date;

/**
 * 绩效录入 / 展示用的数据传输对象。
 *
 * <p>业绩分必填，能力分与态度分可空（为空时按业绩分计算），
 * 综合得分与等级由后端算出后回填。</p>
 *
 * @author Limiti
 */
@Data
public class PerformanceDTO {

    private Long id;

    private Long employeeId;

    /** 员工姓名（列表展示用） */
    private String employeeName;

    /** 工号（列表展示用） */
    private String employeeNumber;

    private Integer year;

    private Integer quarter;

    /** 业绩分，权重 60% */
    private Double score;

    /** 能力分，权重 20% */
    private Double abilityScore;

    /** 态度分，权重 20% */
    private Double attitudeScore;

    /** 综合得分（后端计算） */
    private Double finalScore;

    /** 等级 S/A/B/C/D（后端计算） */
    private String grade;

    private String evaluation;

    private Long evaluatorId;

    private String evaluatorName;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    /** 历史字段：月度业绩，可为空 */
    private Integer month;

    /** 历史字段：旧的工作评分，可为空 */
    private Integer workScore;
}
