package com.limiti.employee.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

/**
 * 公告实体类
 * @author Limiti
 * @date 2024/11/15
 */
@Data
@Entity
@Table(name = "notices")
public class Notice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Admin publisher;

    @Column(nullable = false)
    private Integer status = 0;  // 0-草稿，1-已发布，2-已撤回

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "publish_time")
    private Date publishTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
} 