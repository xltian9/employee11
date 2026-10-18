package com.limiti.employee.service;

import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.AttendanceRepository;
import com.limiti.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 考勤管理服务
 *
 * <p>签到/签退只负责记录原始打卡时间与当天状态；月度统计（迟到次数、旷工天数）
 * 由 {@link AttendanceStatsService} 实时比对工作日算出。</p>
 *
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

    @Autowired
    private SalaryPolicyService salaryPolicyService;

    /**
     * 员工签到
     */
    @Transactional
    public Attendance checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        if (Boolean.TRUE.equals(employee.getIsDel())) {
            throw BusinessException.notFound("员工不存在");
        }

        LocalDate today = LocalDate.now();
        List<Attendance> exists = attendanceRepository
                .findByEmployee_IdAndDate(employeeId, today);
        if (!exists.isEmpty()) {
            throw BusinessException.conflict("今日已签到，无需重复签到");
        }

        SalaryPolicy policy = salaryPolicyService.get();
        LocalTime workStart = parseTime(policy.getWorkStart(), LocalTime.of(9, 0));

        LocalDateTime now = LocalDateTime.now();
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(today);
        attendance.setCheckInTime(now);
        attendance.setStatus(now.toLocalTime().isAfter(workStart)
                ? Attendance.STATUS_LATE : Attendance.STATUS_NORMAL);
        attendance.setNote(now.toLocalTime().isAfter(workStart) ? "签到（迟到）" : "签到成功");
        attendance.setCreateTime(now);
        log.info("员工 [{}] 签到成功，状态 {}", employee.getName(),
                Attendance.statusName(attendance.getStatus()));
        return attendanceRepository.save(attendance);
    }

    /**
     * 员工签退
     */
    @Transactional
    public Attendance checkOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> BusinessException.notFound("考勤记录不存在"));
        if (attendance.getCheckOutTime() != null) {
            throw BusinessException.conflict("今日已经签退，无需重复签退");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);

        SalaryPolicy policy = salaryPolicyService.get();
        LocalTime workStart = parseTime(policy.getWorkStart(), LocalTime.of(9, 0));
        LocalTime workEnd = parseTime(policy.getWorkEnd(), LocalTime.of(18, 0));

        LocalDateTime checkInTime = attendance.getCheckInTime();
        boolean late = checkInTime != null && checkInTime.toLocalTime().isAfter(workStart);
        boolean early = now.toLocalTime().isBefore(workEnd);
        if (late && early) {
            attendance.setStatus(Attendance.STATUS_LATE_AND_EARLY);
        } else if (late) {
            attendance.setStatus(Attendance.STATUS_LATE);
        } else if (early) {
            attendance.setStatus(Attendance.STATUS_EARLY);
        } else {
            attendance.setStatus(Attendance.STATUS_NORMAL);
        }
        log.info("考勤记录 [{}] 签退，状态 {}", attendanceId,
                Attendance.statusName(attendance.getStatus()));
        return attendanceRepository.save(attendance);
    }

    private LocalTime parseTime(String text, LocalTime fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalTime.parse(text.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * 查询员工的考勤记录
     */
    public List<Attendance> findByEmployeeId(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        return attendanceRepository.findByEmployee(employee);
    }
}
