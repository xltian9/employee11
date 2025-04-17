package com.limiti.employee.service;

import com.limiti.employee.entity.Department;
import com.limiti.employee.repository.DepartmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;

/**
 * 部门服务
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class DepartmentService {
    
    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * 获取所有部门
     */
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    /**
     * 根据ID查找部门
     */
    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("部门不存在: " + id));
    }

    /**
     * 创建新部门
     */
    @Transactional
    public Department save(Department department) {
        // 验证部门名称是否已存在
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("部门名称已存在: " + department.getName());
        }
        
        // 设置时间
        department.setCreateTime(new Date());
        department.setUpdateTime(new Date());
        
        return departmentRepository.save(department);
    }

    /**
     * 更新部门信息
     */
    @Transactional
    public Department update(Long id, Department department) {
        Department existingDepartment = findById(id);
        
        // 如果部门名称变更，检查新名称是否已存在
        if (!existingDepartment.getName().equals(department.getName()) &&
            departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("部门名称已存在: " + department.getName());
        }
        
        // 更新信息
        existingDepartment.setName(department.getName());
        existingDepartment.setDescription(department.getDescription());
        existingDepartment.setManager(department.getManager());
        existingDepartment.setUpdateTime(new Date());
        
        return departmentRepository.save(existingDepartment);
    }

    /**
     * 删除部门
     */
    @Transactional
    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }

    /**
     * 搜索部门
     */
    public List<Department> searchDepartments(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return departmentRepository.findByNameContainingOrderByCreateTimeDesc(keyword.trim());
    }
} 