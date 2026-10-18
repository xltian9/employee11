package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.ChangePasswordRequest;
import com.limiti.employee.dto.LoginRequest;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Api(tags = "认证接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    @ApiOperation("登录")
    public Result<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            boolean employeeLogin = "employee".equals(request.getLoginType());

            Result<?> result = employeeLogin
                    ? authService.loginByNameAndPassword(request.getUsername(), request.getPassword())
                    : authService.login(request.getUsername(), request.getPassword());

            if (result.getCode() == null || result.getCode() != 200) {
                return result;
            }

            // 防会话固定攻击：登录成功后丢弃旧会话，重新签发 session
            HttpSession oldSession = httpRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = httpRequest.getSession(true);
            if (employeeLogin) {
                session.setAttribute(SessionUser.EMPLOYEE_KEY, result.getData());
            } else {
                session.setAttribute(SessionUser.ADMIN_KEY, result.getData());
            }
            return result;
        } catch (Exception e) {
            log.error("登录处理失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改当前登录者的登录密码
     */
    @PostMapping("/change-password")
    @ApiOperation("修改密码")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        Admin admin = SessionUser.admin(httpRequest);
        if (admin != null) {
            return authService.changeAdminPassword(admin.getId(),
                    request.getOldPassword(), request.getNewPassword());
        }
        Employee employee = SessionUser.employee(httpRequest);
        if (employee != null) {
            return authService.changeEmployeePassword(employee.getId(),
                    request.getOldPassword(), request.getNewPassword());
        }
        return Result.error(401, "未登录或登录已过期，请重新登录");
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
        return Result.success();
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
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }
}
