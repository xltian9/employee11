package com.limiti.employee.repository;

import com.limiti.employee.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 请假数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    
    /**
     * 查找员工的请假记录
     * @param employeeId 员工ID
     * @return 请假记录列表
     */
    List<Leave> findByEmployee_IdOrderByCreateTimeDesc(Long employeeId);
    
    /**
     * 查找待审批的请假记录
     * @param status 审批状态
     * @return 请假记录列表
     */
    List<Leave> findByStatusOrderByCreateTimeDesc(Integer status);

    /**
     * 按审批状态查找请假记录（工资报告统计用）
     * @param status 审批状态
     * @return 请假记录列表
     */
    List<Leave> findByStatus(Integer status);
} 