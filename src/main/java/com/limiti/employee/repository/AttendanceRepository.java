package com.limiti.employee.repository;

import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployee(Employee employee);
    List<Attendance> findByEmployeeAndCheckInTimeBetween(
        Employee employee, LocalDateTime startTime, LocalDateTime endTime);
} 