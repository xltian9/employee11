package com.limiti.employee.repository;
import com.limiti.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 根据姓名查找未删除的员工（登录用，重名时返回多条）
     */
    List<Employee> findByNameAndIsDel(String name, Boolean isDel);

    /**
     * 不查询被删除的用户
     */
    List<Employee> findByIsDel(Boolean isDel);

    Optional<Employee> findByNameAndPhone(String name, String phone);

    /**
     * 分页查询未删除的员工，支持关键字（姓名/工号模糊）、部门名、在职状态过滤。
     * 三个条件都传 null 时等价于全量分页。
     */
    @Query("SELECT e FROM Employee e WHERE e.isDel = false " +
           "AND (:keyword IS NULL OR e.name LIKE CONCAT('%', :keyword, '%') " +
           "                     OR e.employeeNumber LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:department IS NULL OR e.department = :department) " +
           "AND (:status IS NULL OR e.status = :status)")
    Page<Employee> searchPage(@Param("keyword") String keyword,
                              @Param("department") String department,
                              @Param("status") Boolean status,
                              Pageable pageable);
}