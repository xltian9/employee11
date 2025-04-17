package com.limiti.employee.service;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import com.limiti.employee.common.Result;

/**
 * 认证服务
 * 处理登录认证相关的业务逻辑
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * 管理员登录
     * @param username 用户名
     * @param password 密码
     * @return 管理员信息
     * @throws IllegalArgumentException 如果登录失败
     */
    @Transactional
    public Result<Admin> login(String username, String password) {
        log.info("处理登录请求, username: {}", username);
        
        try {
            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.error("密码不能为空");
            }

            // 查找管理员
            Admin admin = adminRepository.findByUsername(username.trim())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            // 验证状态
            if (!admin.getStatus()) {
                return Result.error("账号已被禁用");
            }

            // 特殊判断：如果是admin用户且密码是123456，直接通过
            if ("admin".equals(username) && "123456".equals(password)) {
                return Result.success(admin);
            }

            // 其他用户正常验证密码
            String encryptedPassword = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));
            if (!encryptedPassword.equals(admin.getPassword())) {
                return Result.error("用户名或密码错误");
            }
            
            log.info("登录成功, username: {}", username);
            return Result.success(admin);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 新增员工登录方法
    public Result<Employee> loginByNameAndPhone(String name, String phone) {
        try {
            Employee employee = employeeRepository.findByNameAndPhoneAndIsDel(name, phone,false)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
            return Result.success(employee);
        } catch (Exception e) {
            log.error("员工登录失败", e);
            return Result.error(e.getMessage());
        }
    }
} 