package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Department;
import com.limiti.employee.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/departments")
@Api(tags = "部门管理接口")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取所有部门
     */
    @GetMapping
    @ApiOperation("获取所有部门")
    public Result<List<Department>> getAllDepartments() {
        try {
            log.info("获取所有部门");
            List<Department> departments = departmentService.findAll();
            return Result.success(departments);
        } catch (Exception e) {
            log.error("获取部门列表失败", e);
            return Result.error("获取部门列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建部门
     */
    @PostMapping
    @ApiOperation("创建部门")
    public Result<Department> createDepartment(@RequestBody Department department) {
        try {
            log.info("创建部门: {}", department.getName());
            Department created = departmentService.save(department);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建部门失败", e);
            return Result.error("创建部门失败：" + e.getMessage());
        }
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @ApiOperation("更新部门")
    public Result<Department> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department) {
        try {
            log.info("更新部门, id: {}", id);
            Department updated = departmentService.update(id, department);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return Result.error("更新部门失败：" + e.getMessage());
        }
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        try {
            log.info("删除部门, id: {}", id);
            departmentService.delete(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return Result.error("删除部门失败：" + e.getMessage());
        }
    }

    /**
     * 搜索部门
     */
    @GetMapping("/search")
    @ApiOperation("搜索部门")
    public Result<List<Department>> searchDepartments(@RequestParam String keyword) {
        try {
            log.info("搜索部门, keyword: {}", keyword);
            List<Department> departments = departmentService.searchDepartments(keyword);
            return Result.success(departments);
        } catch (Exception e) {
            log.error("搜索部门失败", e);
            return Result.error("搜索部门失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取部门
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取部门")
    public Result<Department> getDepartment(@PathVariable Long id) {
        try {
            log.info("获取部门信息, id: {}", id);
            Department department = departmentService.findById(id);
            return Result.success(department);
        } catch (Exception e) {
            log.error("获取部门信息失败", e);
            return Result.error("获取部门信息失败：" + e.getMessage());
        }
    }
} 