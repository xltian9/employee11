package com.limiti.employee.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PerformanceDTO {
    private Long id;
    private Long employeeId;
    private Integer year;
    private Integer quarter;
    private Double score;
    private String evaluation;
    private Long evaluatorId;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private Integer month;
    private Integer workScore;

}
