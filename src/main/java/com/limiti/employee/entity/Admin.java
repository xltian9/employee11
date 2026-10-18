package com.limiti.employee.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * 管理员实体类
 * 用于存储系统管理员的基本信息
 * @author Limiti
 * @date 2024/11/15
 */
@Data
@Entity
@Table(name = "admins")
public class Admin {
    
    /** 管理员ID，主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 登录用户名，唯一标识 */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** 登录密码（加密存储）。WRITE_ONLY：永不随响应返回密文 */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /** 管理员姓名 */
    @Column(name = "name", length = 100)
    private String name;

    /** 联系电话 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 电子邮箱 */
    @Column(name = "email", length = 100)
    private String email;

    /** 账号状态：true-启用，false-禁用 */
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean status = true;

    /** 记录创建时间 */
    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /** 记录更新时间 */
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createTime = now;
        updateTime = now;
        if (status == null) {
            status = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
} 