package com.limiti.employee.service;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.security.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Autowired
    private PasswordService passwordService;

    /**
     * 管理员登录
     * @param username 用户名
     * @param password 密码
     * @return 管理员信息
     */
    @Transactional
    public Result<Admin> login(String username, String password) {
        log.info("处理管理员登录请求, username: {}", username);

        try {
            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.error("密码不能为空");
            }

            // 去除用户名前后空格
            String trimmedUsername = username.trim();
            // 查找管理员
            Admin admin = adminRepository.findByUsername(trimmedUsername)
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
            // 验证状态
            if (!Boolean.TRUE.equals(admin.getStatus())) {
                return Result.error("账号已被禁用");
            }

            // 校验密码（BCrypt，兼容历史 MD5 数据）
            if (!passwordService.matches(password, admin.getPassword())) {
                return Result.error("用户名或密码错误");
            }

            // 历史 MD5 密码在校验通过后自动升级为 BCrypt
            if (passwordService.needsUpgrade(admin.getPassword())) {
                admin.setPassword(passwordService.encode(password));
                adminRepository.save(admin);
                log.info("管理员 [{}] 的密码已升级为 BCrypt", trimmedUsername);
            }

            log.info("登录成功, username: {}", trimmedUsername);
            return Result.success(admin);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 员工登录：姓名 + 密码（BCrypt 校验，密码为空的历史数据回退到手机号）
     *
     * @param name     员工姓名
     * @param password 登录密码
     */
    @Transactional
    public Result<Employee> loginByNameAndPassword(String name, String password) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Result.error("姓名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return Result.error("密码不能为空");
            }

            List<Employee> candidates = employeeRepository.findByNameAndIsDel(name.trim(), false);
            if (candidates.isEmpty()) {
                return Result.error("用户名或密码错误");
            }

            for (Employee employee : candidates) {
                if (!Boolean.TRUE.equals(employee.getStatus())) {
                    continue;
                }
                // 还没有密码的历史数据：允许用手机号登录，并顺便落一个 BCrypt 密码
                if (passwordService.needsUpgrade(employee.getPassword())) {
                    if (employee.getPhone() != null && employee.getPhone().equals(password)) {
                        employee.setPassword(passwordService.encode(password));
                        employeeRepository.save(employee);
                        return Result.success(employee);
                    }
                    continue;
                }
                if (passwordService.matches(password, employee.getPassword())) {
                    return Result.success(employee);
                }
            }

            return Result.error("用户名或密码错误");
        } catch (Exception e) {
            log.error("员工登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改员工密码
     */
    @Transactional
    public Result<Void> changeEmployeePassword(Long employeeId, String oldPassword, String newPassword) {
        String invalid = validateNewPassword(newPassword);
        if (invalid != null) {
            return Result.error(invalid);
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || Boolean.TRUE.equals(employee.getIsDel())) {
            return Result.error("员工不存在");
        }
        if (!verifyOldPassword(employee.getPassword(), employee.getPhone(), oldPassword)) {
            return Result.error("原密码不正确");
        }
        employee.setPassword(passwordService.encode(newPassword));
        employeeRepository.save(employee);
        log.info("员工 [{}] 修改了登录密码", employee.getName());
        return Result.success();
    }

    /**
     * 修改管理员密码
     */
    @Transactional
    public Result<Void> changeAdminPassword(Long adminId, String oldPassword, String newPassword) {
        String invalid = validateNewPassword(newPassword);
        if (invalid != null) {
            return Result.error(invalid);
        }
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        if (!verifyOldPassword(admin.getPassword(), null, oldPassword)) {
            return Result.error("原密码不正确");
        }
        admin.setPassword(passwordService.encode(newPassword));
        adminRepository.save(admin);
        log.info("管理员 [{}] 修改了登录密码", admin.getUsername());
        return Result.success();
    }

    /**
     * 校验新密码规则
     */
    private String validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return "新密码长度不能少于 6 位";
        }
        if (newPassword.length() > 64) {
            return "新密码长度不能超过 64 位";
        }
        return null;
    }

    /**
     * 校验原密码：已加密的走 BCrypt/MD5，历史无密码的数据回退到手机号
     */
    private boolean verifyOldPassword(String stored, String phone, String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        if (passwordService.needsUpgrade(stored)) {
            return phone != null && phone.equals(rawPassword);
        }
        return passwordService.matches(rawPassword, stored);
    }
}
