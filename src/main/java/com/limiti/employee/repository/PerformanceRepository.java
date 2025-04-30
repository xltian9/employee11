package com.limiti.employee.repository;

//import com.limiti.employee.dto.PerformanceInfoDTO;
import com.limiti.employee.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 业绩数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> , JpaSpecificationExecutor<Performance>{
    
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

//    List<PerformanceInfoDTO> list(@Param("params") Map<String, Object> params);
//
    // 使用 @Query 注解自定义查询，这里需要根据实际需求编写 JPQL 语句
//    @Query("SELECT new com.limiti.employee.dto.PerformanceInfoDTO(p.id, p.name, p.score) " +
//    "FROM Performance p " +
//    "WHERE (:params IS NULL OR " +
//    "(:params['employeeId'] IS NULL OR p.employee.id = :#{#params['employeeId']}) AND " +
//    "(:params['year'] IS NULL OR p.year = :#{#params['year']}) AND " +
//    "(:params['quarter'] IS NULL OR p.quarter = :#{#params['quarter']}))")
//List<PerformanceInfoDTO> list(Map<String, Object> params);
}