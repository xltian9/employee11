package com.limiti.employee.entity;
import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工实体类
 * 用于存储员工的基本信息，包括个人信息和工作信息
 * @author Limiti
 * @date 2024/10/15
 */
@Data
@Entity
@Table(name = "employees")
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 员工ID，主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 员工姓名 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 员工工号，唯一标识 */
    @Column(name = "employee_number", nullable = false, length = 50, unique = true)
    private String employeeNumber;

    /** 联系电话 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 电子邮箱 */
    @Column(name = "email", length = 100)
    private String email;

    /** 所属部门 */
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    /** 职位 */
    @Column(name = "position", nullable = false, length = 100)
    private String position;

    /** 员工月薪 */
    @Column(name = "wage", nullable = false, length = 100)
    private BigDecimal wage;

    /** 入职日期 */
    @Column(name = "entry_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    /** 在职状态：true-在职，false-离职 */
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

    
    /** 是否被删除 */
    @Column(name = "is_del", nullable = false)
    private Boolean isDel;

    /**图片存储位置*/
    @Column(name = "photo_path")
    private String photoPath;

//    @Column(name = "photo_base64")
//    private String photoBase64;


    /**
     * 创建记录时的处理
     * 自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
        if (status == null) {
            status = true;
        }
        if (entryDate == null) {
            entryDate = now;
        }
    }

    /**
     * 更新记录时的处理
     * 自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", employeeNumber='" + employeeNumber + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", status=" + status +
                '}';
    }
}
