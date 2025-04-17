package com.limiti.employee.service;

import com.limiti.employee.entity.Performance;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.PerformanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

/**
 * 业绩服务
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class PerformanceService {

    @Autowired
    private PerformanceRepository performanceRepository;

    /**
     * 创建业绩评估
     * @param performance 业绩信息
     * @param employee 员工
     * @param admin 评估人
     * @return 业绩记录
     */
    @Transactional
    public Performance createPerformance(Performance performance, Employee employee, Admin admin) {
        log.info("创建业绩评估: {}, 员工: {}", performance.getMonth(), employee.getName());
        
        // 检查是否已存在该月份的评估
        performanceRepository.findByEmployee_IdAndMonth(employee.getId(), performance.getMonth())
                .ifPresent(p -> {
                    throw new IllegalStateException("该月份已存在业绩评估记录");
                });
        
        performance.setEmployee(employee);
        performance.setEvaluator(admin);
        
        return performanceRepository.save(performance);
    }

    /**
     * 更新业绩评估
     * @param id 业绩记录ID
     * @param performance 更新的业绩信息
     * @return 更新后的业绩记录
     */
    @Transactional
    public Performance updatePerformance(Long id, Performance performance) {
        Performance existingPerformance = findById(id);
        
        existingPerformance.setWorkScore(performance.getWorkScore());
        existingPerformance.setAttitudeScore(performance.getAttitudeScore());
        existingPerformance.setAttendanceScore(performance.getAttendanceScore());
        existingPerformance.setComment(performance.getComment());
        
        log.info("更新业绩评估: {}, 员工: {}", 
                existingPerformance.getMonth(), 
                existingPerformance.getEmployee().getName());
        
        return performanceRepository.save(existingPerformance);
    }

    /**
     * 获取��工的业绩记录
     * @param employeeId 员工ID
     * @return 业绩记录列表
     */
    public List<Performance> getEmployeePerformances(Long employeeId) {
        return performanceRepository.findByEmployee_IdOrderByMonthDesc(employeeId);
    }

    /**
     * 获取部门的业绩记录
     * @param department 部门名称
     * @param month 月份
     * @return 业绩记录列表
     */
    public List<Performance> getDepartmentPerformances(String department, String month) {
        return performanceRepository.findByEmployee_DepartmentAndMonthOrderByTotalScoreDesc(
                department, month);
    }

    /**
     * 根据ID查找业绩记录
     * @param id 业绩记录ID
     * @return 业绩记录
     */
    public Performance findById(Long id) {
        return performanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("业绩记录不存在: " + id));
    }
} 