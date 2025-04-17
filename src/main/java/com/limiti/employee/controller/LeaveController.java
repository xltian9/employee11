package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.service.EmployeeService;
import com.limiti.employee.service.LeaveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * 请假管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/leaves")
@Api(tags = "请假管理接口")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 获取所有请假记录
     */
    @GetMapping
    @ApiOperation("获取所有请假记录")
    public Result<List<Leave>> getAllLeaves() {
        try {
            List<Leave> leaves = leaveService.findAll();
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取请假记录失败", e);
            return Result.error("获取请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审批的请假记录
     */
    @GetMapping("/pending")
    @ApiOperation("获取待审批的请假记录")
    public Result<List<Leave>> getPendingLeaves() {
        try {
            List<Leave> leaves = leaveService.getPendingLeaves();
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取待审批请假记录失败", e);
            return Result.error("获取待审批请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 提交请假申请
     */
    @PostMapping
    @ApiOperation("提交请假申请")
    public Result<Leave> submitLeave(@RequestBody Leave leave, HttpSession session) {
        try {
            // 获取当前登录用户
            Employee employee = (Employee) session.getAttribute("employee");
            Admin admin = (Admin) session.getAttribute("admin");
            
            if (employee == null && admin == null) {
                return Result.error("未登录");
            }

            // 如果是管理员登录，使用管理员信息
            if (admin != null) {
                // 根据管理员信息创建或获取对应的员工记录
                Employee adminEmployee = employeeService.findByName(admin.getUsername())
                    .orElseGet(() -> {
                        Employee newEmployee = new Employee();
                        newEmployee.setName(admin.getUsername());
                        newEmployee.setStatus(true);
                        newEmployee.setDepartment("管理部");
                        newEmployee.setPosition("管理员");
                        newEmployee.setPhone("00000000000");
                        newEmployee.setEntryDate(new Date());
                        newEmployee.setCreateTime(new Date());
                        newEmployee.setUpdateTime(new Date());
                        return employeeService.save(newEmployee);
                    });
                leave.setEmployee(adminEmployee);
            } else {
                // 如果是员工登录，使用员工信息
                leave.setEmployee(employee);
            }

            // 设置请假状态为待审核
            leave.setStatus(0);
            leave.setCreateTime(new Date());
            
            Leave saved = leaveService.save(leave);
            return Result.success(saved);
        } catch (Exception e) {
            log.error("提交请假申请失败", e);
            return Result.error("提交请假申请失败：" + e.getMessage());
        }
    }

    /**
     * 审批请假
     */
    @PostMapping("/{id}/approve")
    @ApiOperation("审批请假")
    public Result<Leave> approveLeave(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String comment) {
        try {
            // TODO: 后续应该从Session中获取
            Admin admin = adminRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("管理员不存在"));
            
            Leave approved = leaveService.approveLeave(id, status, comment, admin);
            return Result.success(approved);
        } catch (Exception e) {
            log.error("审批请假失败", e);
            return Result.error("审批请假失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取请假详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取请假详情")
    public Result<Leave> getLeave(@PathVariable Long id) {
        try {
            Leave leave = leaveService.findById(id);
            return Result.success(leave);
        } catch (Exception e) {
            log.error("获取请假详情失败", e);
            return Result.error("获请假详情失败：" + e.getMessage());
        }
    }

    /**
     * 搜索请假记录
     */
    @GetMapping("/search")
    @ApiOperation("搜索请假记录")
    public Result<List<Leave>> searchLeaves(@RequestParam(required = false) Integer status) {
        try {
            List<Leave> leaves;
            if (status != null) {
                leaves = leaveService.findByStatus(status);
            } else {
                leaves = leaveService.findAll();
            }
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("搜索请假记录失败", e);
            return Result.error("搜索请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定员工的请假记录
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取指定员工的请假记录")
    public Result<List<Leave>> getEmployeeLeaves(@PathVariable Long employeeId) {
        try {
            // 验证员工是否存在
            Employee employee = employeeService.findById(employeeId);
            
            // 获取该员工的请假记录
            List<Leave> leaves = leaveService.getEmployeeLeaves(employeeId);
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取员工请假记录失败", e);
            return Result.error("获取员工请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录员工的请假记录
     */
    @GetMapping("/my")
    @ApiOperation("获取我的请假记录")
    public Result<List<Leave>> getMyLeaves(HttpSession session) {
        try {
            // 获取当前登录用户
            Employee employee = (Employee) session.getAttribute("employee");
            Admin admin = (Admin) session.getAttribute("admin");
            
            if (employee == null && admin == null) {
                return Result.error("未登录");
            }

            Long employeeId;
            if (admin != null) {
                // 如果是管理员，查找对应的员工记录
                Employee adminEmployee = employeeService.findByName(admin.getUsername())
                    .orElseThrow(() -> new RuntimeException("未找到管理员对应的员工记录"));
                employeeId = adminEmployee.getId();
            } else {
                employeeId = employee.getId();
            }

            List<Leave> leaves = leaveService.getEmployeeLeaves(employeeId);
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取我的请假记录失败", e);
            return Result.error("获取我的请假记录失败：" + e.getMessage());
        }
    }
} 