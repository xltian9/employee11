package com.limiti.employee.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工资调整条目（补发 / 扣款），随工资明细一起返回，方便前端展开查看原因。
 *
 * @author Limiti
 */
@Data
public class SalaryAdjustmentDTO {

    private Long id;
    private Integer type;
    private String typeName;
    private BigDecimal amount;
    private String reason;
    private String operatorName;
    private String createTime;
}
