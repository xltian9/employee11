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
    List<Performance> findByEmployee_IdOrderByYearDescQuarterDesc(Long employeeId);
    
    /**
     * 查找员工指定年份和季度的业绩记录
     * @param employeeId 员工ID
     * @param year 年份
     * @param quarter 季度
     * @return 业绩记录
     */
    Optional<Performance> findByEmployee_IdAndYearAndQuarter(Long employeeId, Integer year, Integer quarter);
    
    /**
     * 查找指定年份和季度的所有业绩记录
     * @param year 年份
     * @param quarter 季度
     * @return 业绩记录列表
     */
    List<Performance> findByYearAndQuarterOrderByScoreDesc(Integer year, Integer quarter);
    
    /**
     * 查找指定评估人的业绩记录
     * @param evaluatorId 评估人ID
     * @return 业绩记录列表
     */
    List<Performance> findByEvaluator_IdOrderByCreateTimeDesc(Long evaluatorId);
    
    /**
     * 查找指定部门的业绩记录
     * @param department 部门名称
     * @param year 年份
     * @param quarter 季度
     * @return 业绩记录列表
     */
    List<Performance> findByEmployee_DepartmentAndYearAndQuarterOrderByScoreDesc(
        String department, Integer year, Integer quarter);
} 