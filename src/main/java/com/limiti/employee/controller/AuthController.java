package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.ChangePasswordRequest;
import com.limiti.employee.dto.LoginRequest;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AuditLogService;
import com.limiti.employee.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 *
 * <p>登录失败、改密失败都通过 {@link BusinessException} 抛出，由全局处理器统一转成
 * 4xx 状态码 + 统一 msg，前端只写一套处理逻辑。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@RestController
@RequestMapping("/api/auth")
@Api(tags = "认证接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    @ApiOperation("登录")
    public Result<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        boolean employeeLogin = "employee".equals(request.getLoginType());
        String operatorName = request.getUsername() == null ? "" : request.getUsername().trim();

        Result<?> result;
        try {
            if (employeeLogin) {
                Employee employee = authService.loginByNameAndPassword(request.getUsername(), request.getPassword());
                result = Result.success(employee);
                // 防会话固定攻击：登录成功后丢弃旧会话，重新签发 session
                HttpSession oldSession = httpRequest.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                httpRequest.getSession(true).setAttribute(SessionUser.EMPLOYEE_KEY, employee);
                auditLogService.record(AuditLogService.LOGIN, "登录成功",
                        "AUTH", employee.getId(), employee.getName(), "员工登录");
            } else {
                Admin admin = authService.login(request.getUsername(), request.getPassword());
                result = Result.success(admin);
                HttpSession oldSession = httpRequest.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                httpRequest.getSession(true).setAttribute(SessionUser.ADMIN_KEY, admin);
                auditLogService.record(AuditLogService.LOGIN, "登录成功",
                        "AUTH", admin.getId(), admin.getUsername(), "管理员登录");
            }
        } catch (BusinessException e) {
            auditLogService.record(AuditLogService.LOGIN, "登录失败",
                    "AUTH", null, operatorName, e.getMessage());
            throw e;
        }
        return result;
    }

    /**
     * 修改当前登录者的登录密码
     */
    @PostMapping("/change-password")
    @ApiOperation("修改密码")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        if (request.getOldPassword() == null || request.getNewPassword() == null) {
            throw BusinessException.badRequest("请填写原密码与新密码");
        }
        Admin admin = SessionUser.admin(httpRequest);
        if (admin != null) {
            authService.changeAdminPassword(admin.getId(), request.getOldPassword(), request.getNewPassword());
            auditLogService.record(AuditLogService.PASSWORD_CHANGE, "修改密码",
                    "ADMIN", admin.getId(), admin.getUsername(), "管理员自助修改密码");
            return Result.success("密码修改成功", null);
        }
        Employee employee = SessionUser.employee(httpRequest);
        if (employee != null) {
            authService.changeEmployeePassword(employee.getId(), request.getOldPassword(), request.getNewPassword());
            auditLogService.record(AuditLogService.PASSWORD_CHANGE, "修改密码",
                    "EMPLOYEE", employee.getId(), employee.getName(), "员工自助修改密码");
            return Result.success("密码修改成功", null);
        }
        throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success("已退出登录", null);
    }

    /**
     * 获取当前登录者信息（前端判活/刷新用）
     */
    @GetMapping("/me")
    @ApiOperation("获取当前登录者")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        Admin admin = SessionUser.admin(request);
        if (admin != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "admin");
            data.put("id", admin.getId());
            data.put("name", admin.getName());
            data.put("username", admin.getUsername());
            return Result.success(data);
        }
        Employee employee = SessionUser.employee(request);
        if (employee != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "employee");
            data.put("id", employee.getId());
            data.put("name", employee.getName());
            data.put("employeeNumber", employee.getEmployeeNumber());
            data.put("department", employee.getDepartment());
            return Result.success(data);
        }
        throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
    }
}
