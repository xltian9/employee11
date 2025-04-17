package com.limiti.employee.service;

import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.AttendanceRepository;
import com.limiti.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤管理服务
 * 处理员工考勤相关的业务逻辑
 * @author Limiti
 * @date 2024/11/30
 */
@Slf4j
@Service
public class AttendanceService {
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    /**
     * 员工签到
     * @param employeeId 员工ID
     * @return 考勤记录
     */
    @Transactional
    public Attendance checkIn(Long employeeId) {
        log.info("处理员工签到, employeeId: {}", employeeId);
        
        // 查找员工
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("员工不存在: " + employeeId));
        
        // 检查今日是否已签到
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = today.plusDays(1);
        List<Attendance> todayAttendances = attendanceRepository
                .findByEmployeeAndCheckInTimeBetween(employee, today, tomorrow);
        
        if (!todayAttendances.isEmpty()) {
            throw new IllegalStateException("今日已签到");
        }
        
        // 创建新的考勤记录
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus("正常");
        attendance.setNote("签到成功");
        attendance.setCreateTime(LocalDateTime.now());
        
        log.info("保存签到记录: {}", attendance);
        return attendanceRepository.save(attendance);
    }
    
    /**
     * 员工签退
     * @param attendanceId 考勤记录ID
     * @return 更新后的考勤记录
     */
    @Transactional
    public Attendance checkOut(Long attendanceId) {
        log.info("处理员工签退, attendanceId: {}", attendanceId);
        
        // 查找考勤记录
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new EntityNotFoundException("考勤记录不存在: " + attendanceId));
        
        // 检查是否已签退
        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("已经签退");
        }
        
        // 更新签退时间和状态
        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);
        
        // 计算工作时长，设置考勤状态
        LocalDateTime checkInTime = attendance.getCheckInTime();
        if (checkInTime != null) {
            if (checkInTime.getHour() >= 9 && checkInTime.getMinute() > 0) {
                attendance.setStatus("迟到");
            }
            if (now.getHour() < 18) {
                attendance.setStatus("早退");
            }
        }
        
        log.info("更新签退记录: {}", attendance);
        return attendanceRepository.save(attendance);
    }
    
    /**
     * 查询员工的考勤记录
     * @param employeeId 员工ID
     * @return 考勤记录列表
     */
    public List<Attendance> findByEmployeeId(Long employeeId) {
        log.info("查询员工考勤记录, employeeId: {}", employeeId);
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("员工不存在: " + employeeId));
                
        return attendanceRepository.findByEmployee(employee);
    }
} 