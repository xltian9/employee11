package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.service.PerformanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业绩管理控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/performances")
@Api(tags = "业绩管理接口")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    /**
     * 创建业绩评估
     */
    @PostMapping
    @ApiOperation("创建业绩评估")
    public Result<Performance> createPerformance(
            @RequestBody Performance performance,
            @RequestParam Long employeeId) {
        try {
            // TODO: 从session获取当前管理员信息
            Admin admin = getCurrentAdmin();
            Employee employee = getEmployee(employeeId);
            Performance created = performanceService.createPerformance(performance, employee, admin);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建业绩评估失败", e);
            return Result.error("创建业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 更新业绩评估
     */
    @PutMapping("/{id}")
    @ApiOperation("更新业绩评估")
    public Result<Performance> updatePerformance(
            @PathVariable Long id,
            @RequestBody Performance performance) {
        try {
            Performance updated = performanceService.updatePerformance(id, performance);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新业绩评估失败", e);
            return Result.error("更新业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工的业绩记录
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取员工的业绩记录")
    public Result<List<Performance>> getEmployeePerformances(@PathVariable Long employeeId) {
        try {
            List<Performance> performances = performanceService.getEmployeePerformances(employeeId);
            return Result.success(performances);
        } catch (Exception e) {
            log.error("获取员工业绩记录失败", e);
            return Result.error("获取员工业绩记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门的业绩记录
     */
    @GetMapping("/department")
    @ApiOperation("获取部门的业绩记录")
    public Result<List<Performance>> getDepartmentPerformances(
            @RequestParam String department,
            @RequestParam String month) {
        try {
            List<Performance> performances = performanceService.getDepartmentPerformances(department, month);
            return Result.success(performances);
        } catch (Exception e) {
            log.error("获取部门业绩记录失败", e);
            return Result.error("获取部门业绩记录失败：" + e.getMessage());
        }
    }

    // TODO: 实现获取当前管理员和员工的方法
    private Admin getCurrentAdmin() {
        return null;
    }

    private Employee getEmployee(Long employeeId) {
        return null;
    }
} 