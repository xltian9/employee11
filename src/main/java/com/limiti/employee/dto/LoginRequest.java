package com.limiti.employee.dto;

import lombok.Data;

/**
 * 登录请求参数类
 * @author Limiti
 * @date 2024/11/15
 */
@Data
public class LoginRequest {
    /**
     * 登录类型：employee 或 admin
     */
    private String loginType;
    
    /**
     * 用户名/姓名
     */
    private String username;
    
    /**
     * 密码/手机号
     */
    private String password;
} 