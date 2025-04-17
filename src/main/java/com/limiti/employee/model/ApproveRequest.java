package com.limiti.employee.model;

import lombok.Data;

/**
 * 请假审批请求
 */
@Data
public class ApproveRequest {
    /** 审批状态：1-通过，2-拒绝 */
    private Integer status;
    
    /** 审批意见 */
    private String comment;
} 