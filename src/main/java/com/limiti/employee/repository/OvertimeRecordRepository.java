package com.limiti.employee.repository;

import com.limiti.employee.entity.OvertimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRecordRepository extends JpaRepository<OvertimeRecord, Long> {

    List<OvertimeRecord> findByWorkDateBetween(LocalDate from, LocalDate to);

    List<OvertimeRecord> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate from, LocalDate to);
}
