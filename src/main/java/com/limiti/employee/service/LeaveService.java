package com.limiti.employee.service;

import com.limiti.employee.entity.Leave;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;

/**
 * 请假服务
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    /**
     * 申请请假
     */
    @Transactional
    public Leave applyLeave(Leave leave, Employee employee) {
        leave.setEmployee(employee);
        leave.setStatus(0); // 待审核状态
        leave.setCreateTime(new Date());
        leave.setUpdateTime(new Date());
        
        // 验证请假时间
        if (leave.getStartTime().after(leave.getEndTime())) {
            throw new IllegalArgumentException("开始时���不能晚于结束时间");
        }
        
        return leaveRepository.save(leave);
    }

    /**
     * 审批请假
     */
    @Transactional
    public Leave approveLeave(Long id, Integer status, String comment, Admin admin) {
        Leave leave = findById(id);
        
        leave.setStatus(status);
        leave.setApprover(admin);
        leave.setComment(comment);
        leave.setApproveTime(new Date());
        leave.setUpdateTime(new Date());
        
        return leaveRepository.save(leave);
    }

    /**
     * 根据ID查找请假记录
     */
    public Leave findById(Long id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("请假记录不存在: " + id));
    }

    /**
     * 获取员工的请假记录
     */
    public List<Leave> getEmployeeLeaves(Long employeeId) {
        return leaveRepository.findByEmployee_IdOrderByCreateTimeDesc(employeeId);
    }

    /**
     * 获取待审批的请假记录
     */
    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatusOrderByCreateTimeDesc(0);
    }

    /**
     * 获取所有请假记录
     */
    public List<Leave> findAll() {
        return leaveRepository.findAll();
    }

    /**
     * 根据状态查找请假记录
     */
    public List<Leave> findByStatus(Integer status) {
        return leaveRepository.findByStatusOrderByCreateTimeDesc(status);
    }

    /**
     * 保存请假记录
     */
    @Transactional
    public Leave save(Leave leave) {
        if (leave.getEmployee() == null) {
            throw new IllegalArgumentException("员工信息不能为空");
        }
        return leaveRepository.save(leave);
    }
} 