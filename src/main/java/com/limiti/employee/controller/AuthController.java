package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.LoginRequest;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
            if ("employee".equals(request.getLoginType())) {
                Result<Employee> result = authService.loginByNameAndPhone(request.getUsername(), request.getPassword());
                if (result.getCode() == 200) {
                    // 将员工信息存入session
                    HttpSession session = httpRequest.getSession();
                    session.setAttribute("employee", result.getData());
                }
                return result;
            } else {
                Result<Admin> result = authService.login(request.getUsername(), request.getPassword());
                if (result.getCode() == 200) {
                    // 将管理员信息存入session
                    HttpSession session = httpRequest.getSession();
                    session.setAttribute("admin", result.getData());
                }
                return result;
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        /*
        // 1. 验证用户名密码（原有逻辑）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // 2. 生成Token（替代Session创建）
        String token = JwtUtils.generateToken(request.getUsername());

        return ResponseEntity.ok().body(Map.of("token", token));
    }*/
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate();
        return Result.success();
    }
} 