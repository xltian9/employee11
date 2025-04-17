package com.limiti.employee.repository;

import com.limiti.employee.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公告数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    /**
     * 查找已发布的公告
     * @return 公告列表
     */
    List<Notice> findByStatusOrderByCreateTimeDesc(Integer status);
    
    /**
     * 根据标题模糊查询
     * @param title 标题关键字
     * @return 公告列表
     */
    List<Notice> findByTitleContainingOrderByCreateTimeDesc(String title);
    
    /**
     * 查找指定管理员发布的公告
     */
    List<Notice> findByPublisherId(Long publisherId);
} 