package com.limiti.employee.repository;

import com.limiti.employee.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业绩数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    
    /**
     * 查找员工的所有业绩记录
     * @param employeeId 员工ID
     * @return 业绩记录列表
     */
    List<Performance> findByEmployee_IdOrderByMonthDesc(Long employeeId);
    
    /**
     * 查找员工指定月份的业绩记录
     * @param employeeId 员工ID
     * @param month 月份（YYYY-MM）
     * @return 业绩记录
     */
    Optional<Performance> findByEmployee_IdAndMonth(Long employeeId, String month);
    
    /**
     * 查找指定月份的所有业绩记录
     * @param month 月份（YYYY-MM）
     * @return 业绩记录列表
     */
    List<Performance> findByMonthOrderByTotalScoreDesc(String month);
    
    /**
     * 查找指定评估人的业绩记录
     * @param evaluatorId 评估人ID
     * @return 业绩记录列表
     */
    List<Performance> findByEvaluator_IdOrderByCreateTimeDesc(Long evaluatorId);
    
    /**
     * 查找指定部门的业绩记录
     * @param department 部门名称
     * @param month 月份（YYYY-MM）
     * @return 业绩记录列表
     */
    List<Performance> findByEmployee_DepartmentAndMonthOrderByTotalScoreDesc(
        String department, String month);
} 