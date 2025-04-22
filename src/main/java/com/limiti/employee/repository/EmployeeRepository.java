package com.limiti.employee.repository;
import com.limiti.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 员工数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    /**
     * 根据工号查找员工
     */
    boolean existsByEmployeeNumber(String employeeNumber);
    
    /**
     * 根据名字或工号或部门模糊搜索
     */
    List<Employee> findByNameContainingOrEmployeeNumberContainingOrDepartmentContainingAndIsDel(
        String name, String employeeNumber, String department,Boolean isDel);
    
    /**
     * 根据状态和部门，并且名字或工号包含关键字的查询
     */
    List<Employee> findByStatusAndDepartmentAndNameContainingOrStatusAndDepartmentAndEmployeeNumberContainingAndIsDel(
        Boolean status1, String department1, String name,
        Boolean status2, String department2, String employeeNumber, Boolean isDel);
    
    /**
     * 根据状态，并且名字或工号包含关键字的查询
     */
    List<Employee> findByStatusAndNameContainingOrStatusAndEmployeeNumberContainingAndIsDel(
        Boolean status1, String name,
        Boolean status2, String employeeNumber, Boolean isDel);
    
    /**
     * 根据部门，并且名字或工号包含关键字的查询
     */
    List<Employee> findByDepartmentAndNameContainingOrDepartmentAndEmployeeNumberContainingAndIsDel(
        String department1, String name,
        String department2, String employeeNumber, Boolean isDel);
    
    /**
     * 根据姓名和手机号查询员工
     */
    Optional<Employee> findByNameAndPhoneAndIsDel(String name, String phone, Boolean isDel);
    
    /**
     * 根据前缀查询最大工号
     */
    @Query("SELECT MAX(e.employeeNumber) FROM Employee e WHERE e.employeeNumber LIKE :prefix%")
    Optional<String> findMaxEmployeeNumberByPrefix(@Param("prefix") String prefix);
    
    /**
     * 根据姓名查找员工
     */
    Optional<Employee> findByName(String name);

    /**
     * 不查询被删除的用户
     */
    List<Employee> findByIsDel(Boolean isDel);

    Optional<Employee> findByNameAndPhone(String name, String phone);
}