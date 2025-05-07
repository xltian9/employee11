package com.limiti.employee.service;

import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 员工管理服务
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class EmployeeService {
    
    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * 获取所有员工
     */
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    /**
     * 根据ID查找员工
     */
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("员工不存在: " + id));
    }

    /**
     * 创建新员工
     */
    @Transactional
    public Employee save(Employee employee) {
        // 如果是新员工，生成工号
        if (employee.getId() == null) {
            employee.setEmployeeNumber(generateEmployeeNumber());
        }
        // 默认为不删除
        employee.setIsDel(false);
        return employeeRepository.save(employee);
    }

    /**
     * 生成工号
     * 格式：EMP + 年份后两位 + 5位序号，例如：EMP2300001
     */
    private String generateEmployeeNumber() {
        // 获取当前年份后两位
        String yearSuffix = String.format("%02d", 
            LocalDate.now().getYear() % 100);
        
        // 获取当天最大工号
        String prefix = "EMP" + yearSuffix;
        String maxNumber = employeeRepository
            .findMaxEmployeeNumberByPrefix(prefix)
            .orElse(prefix + "00000");
        
        // 提取序号并加1
        int sequence = Integer.parseInt(maxNumber.substring(5)) + 1;
        
        // 格式化新工号
        return String.format("%s%05d", prefix, sequence);
    }

    /**
     * 更新员工信息
     */
    @Transactional
    public Employee update(Long id, Employee employee) {
        // 获取现有员工信息
        Employee existingEmployee = findById(id);
        
        // 只更新允许修改的字段
        existingEmployee.setName(employee.getName());
        existingEmployee.setDepartment(employee.getDepartment());
        existingEmployee.setPosition(employee.getPosition());
        existingEmployee.setPhone(employee.getPhone());
        existingEmployee.setEmail(employee.getEmail());
        existingEmployee.setWage(employee.getWage());
        existingEmployee.setEntryDate(employee.getEntryDate());
        existingEmployee.setStatus(employee.getStatus());
        existingEmployee.setUpdateTime(new Date());
        
        // 保存更新
        return employeeRepository.save(existingEmployee);
    }

    /**
     * 删除员工
     */
    @Transactional
    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    /**
     * 搜索员工
     */
    public List<Employee> searchEmployees(String keyword, String department, Boolean status) {
        log.info("搜索员工, keyword: {}, department: {}, status: {}", keyword, department, status);
        
        // 如果没有任何搜索条件，返回所有员工
        if ((keyword == null || keyword.trim().isEmpty()) 
            && (department == null || department.trim().isEmpty())
            && status == null) {
            return findUserList();
        }
        
        // 构建搜索条件
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchDepartment = department == null ? "" : department.trim();
        
        // 根据不同的条件组合调用不同的查询方法
        if (status != null) {
            if (!searchDepartment.isEmpty()) {
                // 状态 + 部门 + 关键字
                return employeeRepository.findByStatusAndDepartmentAndNameContainingOrStatusAndDepartmentAndEmployeeNumberContainingAndIsDel(
                    status, searchDepartment, searchKeyword,
                    status, searchDepartment, searchKeyword, false);
            } else {
                // 状态 + 关键字
                return employeeRepository.findByStatusAndNameContainingOrStatusAndEmployeeNumberContainingAndIsDel(
                    status, searchKeyword,
                    status, searchKeyword,false);
            }
        } else {
            if (!searchDepartment.isEmpty()) {
                // 部门 + 关键字
                return employeeRepository.findByDepartmentAndNameContainingOrDepartmentAndEmployeeNumberContainingAndIsDel(
                    searchDepartment, searchKeyword,
                    searchDepartment, searchKeyword,false);
            } else {
                // 只有关键字
                return employeeRepository.findByNameContainingOrEmployeeNumberContainingOrDepartmentContainingAndIsDel(
                    searchKeyword, searchKeyword, searchKeyword,false);
            }
        }
    }

    /**
     * 根据姓名查找员工
     */
    public Optional<Employee> findByName(String name) {
        return employeeRepository.findByName(name);
    }

    public List<Employee> findUserList()
    {
        return employeeRepository.findByIsDel(false);
    }

    // 获取照片路径
    public String getPhotoPath(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("员工不存在"));
        return employee.getPhotoPath();
    }

    // 更新照片路径
    public void updateEmployeePhoto(Long id, String photoPath) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("员工不存在"));
        employee.setPhotoPath(photoPath);
        employeeRepository.save(employee);
    }

    public Employee getEmployeeById(Long employeeId) {
        // 调用 EmployeeRepository 根据 employeeId 查询员工信息
        return employeeRepository.findById(employeeId).orElse(null);
    }

}