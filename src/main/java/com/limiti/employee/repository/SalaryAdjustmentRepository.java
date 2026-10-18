package com.limiti.employee.repository;

import com.limiti.employee.entity.SalaryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAdjustmentRepository extends JpaRepository<SalaryAdjustment, Long> {

    List<SalaryAdjustment> findByYearAndMonth(Integer year, Integer month);

    List<SalaryAdjustment> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    List<SalaryAdjustment> findByEmployeeIdOrderByCreateTimeDesc(Long employeeId);
}
