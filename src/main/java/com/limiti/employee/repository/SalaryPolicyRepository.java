package com.limiti.employee.repository;

import com.limiti.employee.entity.SalaryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryPolicyRepository extends JpaRepository<SalaryPolicy, Long> {
}
