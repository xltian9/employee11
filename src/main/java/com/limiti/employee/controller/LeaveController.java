package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.LeaveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假管理控制器
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/leaves")
@Api(tags = "请假管理接口")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping
    @ApiOperation("获取所有请假记录")
    public Result<List<Leave>> getAllLeaves(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(leaveService.findAll());
    }

    @GetMapping("/pending")
    @ApiOperation("获取待审批的请假记录")
    public Result<List<Leave>> getPendingLeaves(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(leaveService.getPendingLeaves());
    }

    @PostMapping
    @ApiOperation("提交请假申请")
    public Result<Leave> submitLeave(@RequestBody Leave leave, HttpServletRequest request) {
        Employee employee = SessionUser.requireSelf(request);
        return Result.success("提交成功，等待管理员审批", leaveService.applyLeave(leave, employee));
    }

    @PostMapping("/{id}/approve")
    @ApiOperation("审批请假")
    public Result<Leave> approveLeave(@PathVariable Long id,
                                      @RequestParam Integer status,
                                      @RequestParam(required = false, defaultValue = "") String comment,
                                      HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        return Result.success("审批完成", leaveService.approveLeave(id, status, comment, admin));
    }

    /**
     * 撤销请假：本人或管理员
     */
    @PostMapping("/{id}/cancel")
    @ApiOperation("撤销请假")
    public Result<Leave> cancelLeave(@PathVariable Long id,
                                     @RequestParam(required = false, defaultValue = "") String reason,
                                     HttpServletRequest request) {
        Employee employee = SessionUser.employee(request);
        Admin admin = SessionUser.admin(request);
        if (employee == null && admin == null) {
            throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
        }
        return Result.success("已撤销", leaveService.cancelLeave(id, employee, admin, reason));
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID获取请假详情")
    public Result<Leave> getLeave(@PathVariable Long id, HttpServletRequest request) {
        Leave leave = leaveService.findById(id);
        Long ownerId = leave.getEmployee() == null ? null : leave.getEmployee().getId();
        SessionUser.requireAccessEmployee(request, ownerId);
        return Result.success(leave);
    }

    @GetMapping("/search")
    @ApiOperation("搜索请假记录")
    public Result<List<Leave>> searchLeaves(@RequestParam(required = false) Integer status,
                                            HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(status == null ? leaveService.findAll() : leaveService.findByStatus(status));
    }

    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取指定员工的请假记录")
    public Result<List<Leave>> getEmployeeLeaves(@PathVariable Long employeeId, HttpServletRequest request) {
        SessionUser.requireAccessEmployee(request, employeeId);
        return Result.success(leaveService.getEmployeeLeaves(employeeId));
    }

    @GetMapping("/my")
    @ApiOperation("获取我的请假记录")
    public Result<List<Leave>> getMyLeaves(HttpServletRequest request) {
        Employee employee = SessionUser.employee(request);
        if (employee == null) {
            if (SessionUser.admin(request) == null) {
                throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
            }
            // 管理员没有请假记录，返回空列表而不是报错
            return Result.success(Collections.emptyList());
        }
        return Result.success(leaveService.getEmployeeLeaves(employee.getId()));
    }

    /**
     * 请假类型 / 状态字典，供前端下拉使用
     */
    @GetMapping("/meta")
    public Result<Map<String, Object>> meta() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, String> types = new LinkedHashMap<>();
        types.put("1", "事假");
        types.put("2", "病假");
        types.put("3", "年假");
        types.put("4", "调休");
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("0", "待审批");
        statuses.put("1", "已通过");
        statuses.put("2", "已拒绝");
        statuses.put("3", "已撤销");
        map.put("types", types);
        map.put("statuses", statuses);
        return Result.success(map);
    }
}
