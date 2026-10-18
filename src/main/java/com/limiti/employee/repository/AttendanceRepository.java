package com.limiti.employee.repository;

import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployee(Employee employee);

    List<Attendance> findByEmployeeAndCheckInTimeBetween(
            Employee employee, LocalDateTime startTime, LocalDateTime endTime);

    /** 某月全部员工的考勤记录（工资报告批量统计用，避免逐个员工查询） */
    List<Attendance> findByCheckInTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /** 员工某天的考勤记录（用于判断当天是否已打卡） */
    List<Attendance> findByEmployee_IdAndCheckInTimeBetween(
            Long employeeId, LocalDateTime startTime, LocalDateTime endTime);

    /** 员工指定日期（date 列）的考勤记录 */
    List<Attendance> findByEmployee_IdAndDate(Long employeeId, LocalDate date);
}
