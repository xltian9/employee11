package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.EmployeeService;
import com.limiti.employee.service.LeaveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
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
    private EmployeeService employeeService;

    /**
     * 获取所有请假记录（仅管理员）
     */
    @GetMapping
    @ApiOperation("获取所有请假记录")
    public Result<List<Leave>> getAllLeaves(HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权查看全部请假记录");
        }
        try {
            List<Leave> leaves = leaveService.findAll();
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取请假记录失败", e);
            return Result.error("获取请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审批的请假记录（仅管理员）
     */
    @GetMapping("/pending")
    @ApiOperation("获取待审批的请假记录")
    public Result<List<Leave>> getPendingLeaves(HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权查看待审批记录");
        }
        try {
            List<Leave> leaves = leaveService.getPendingLeaves();
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取待审批请假记录失败", e);
            return Result.error("获取待审批请假记录失败：" + e.getMessage());
        }
    }

    /**
     * 提交请假申请（仅员工本人；管理员需用员工账号提交）
     */
    @PostMapping
    @ApiOperation("提交请假申请")
    public Result<Leave> submitLeave(@RequestBody Leave leave, HttpServletRequest request) {
        Employee employee = SessionUser.employee(request);
        if (employee == null) {
            return Result.error(403, "管理员账号不能提交请假申请，请使用员工账号登录");
        }
        try {
            // 忽略请求体里的 employeeId，一律以 session 中的登录者为准
            leave.setId(null);
            leave.setEmployee(employee);
            leave.setApprover(null);
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
     * 审批请假（仅管理员）
     */
    @PostMapping("/{id}/approve")
    @ApiOperation("审批请假")
    public Result<Leave> approveLeave(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String comment,
            HttpServletRequest request) {
        Admin admin = SessionUser.admin(request);
        if (admin == null) {
            return Result.error(403, "只有管理员才能审批请假");
        }
        try {
            Leave approved = leaveService.approveLeave(id, status, comment, admin);
            return Result.success(approved);
        } catch (Exception e) {
            log.error("审批请假失败", e);
            return Result.error("审批请假失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取请假详情（管理员，或请假人本人）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取请假详情")
    public Result<Leave> getLeave(@PathVariable Long id, HttpServletRequest request) {
        try {
            Leave leave = leaveService.findById(id);
            if (leave == null) {
                return Result.error(404, "请假记录不存在");
            }
            Long ownerId = leave.getEmployee() == null ? null : leave.getEmployee().getId();
            if (!SessionUser.canAccessEmployee(request, ownerId)) {
                return Result.error(403, "无权查看他人的请假记录");
            }
            return Result.success(leave);
        } catch (Exception e) {
            log.error("获取请假详情失败", e);
            return Result.error("获请假详情失败：" + e.getMessage());
        }
    }

    /**
     * 搜索请假记录（仅管理员）
     */
    @GetMapping("/search")
    @ApiOperation("搜索请假记录")
    public Result<List<Leave>> searchLeaves(@RequestParam(required = false) Integer status,
                                            HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权搜索请假记录");
        }
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
     * 获取指定员工的请假记录（管理员，或员工本人）
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取指定员工的请假记录")
    public Result<List<Leave>> getEmployeeLeaves(@PathVariable Long employeeId,
                                                 HttpServletRequest request) {
        if (!SessionUser.canAccessEmployee(request, employeeId)) {
            return Result.error(403, "无权查看其他员工的请假记录");
        }
        try {
            employeeService.findById(employeeId);
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
    public Result<List<Leave>> getMyLeaves(HttpServletRequest request) {
        try {
            Employee employee = SessionUser.employee(request);
            if (employee == null) {
                Admin admin = SessionUser.admin(request);
                if (admin == null) {
                    return Result.error(401, "未登录");
                }
                // 管理员没有请假记录，返回空列表而不是报错
                return Result.success(Collections.emptyList());
            }
            List<Leave> leaves = leaveService.getEmployeeLeaves(employee.getId());
            return Result.success(leaves);
        } catch (Exception e) {
            log.error("获取我的请假记录失败", e);
            return Result.error("获取我的请假记录失败：" + e.getMessage());
        }
    }
}
