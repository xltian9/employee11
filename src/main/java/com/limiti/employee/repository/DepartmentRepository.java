package com.limiti.employee.repository;

import com.limiti.employee.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部门数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    /**
     * 根据部门名称查找
     */
    boolean existsByName(String name);
    
    /**
     * 根据名称精确查找（员工写入时用来把部门名解析成 department_id）
     */
    Optional<Department> findByName(String name);

    /**
     * 根据名称模糊查询
     */
    List<Department> findByNameContainingOrderByCreateTimeDesc(String name);
} 