package com.limiti.employee.service;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.security.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证服务
 *
 * <p>业务失败一律抛 {@link BusinessException}，不再返回带错误码的 Result，
 * 由全局异常处理器统一转成 HTTP 状态码 + 统一消息体。</p>
 *
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
     */
    @Transactional
    public Admin login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw BusinessException.badRequest("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw BusinessException.badRequest("密码不能为空");
        }
        String trimmedUsername = username.trim();
        Admin admin = adminRepository.findByUsername(trimmedUsername)
                .orElseThrow(() -> BusinessException.badRequest("用户名或密码错误"));
        if (!Boolean.TRUE.equals(admin.getStatus())) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        if (!passwordService.matches(password, admin.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        // 历史 MD5 密码在校验通过后自动升级为 BCrypt
        if (passwordService.needsUpgrade(admin.getPassword())) {
            admin.setPassword(passwordService.encode(password));
            adminRepository.save(admin);
            log.info("管理员 [{}] 的密码已升级为 BCrypt", trimmedUsername);
        }
        log.info("管理员登录成功, username: {}", trimmedUsername);
        return admin;
    }

    /**
     * 员工登录：姓名 + 密码（BCrypt 校验，密码为空的历史数据回退到手机号）
     */
    @Transactional
    public Employee loginByNameAndPassword(String name, String password) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("姓名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw BusinessException.badRequest("密码不能为空");
        }
        List<Employee> candidates = employeeRepository.findByNameAndIsDel(name.trim(), false);
        for (Employee employee : candidates) {
            if (!Boolean.TRUE.equals(employee.getStatus())) {
                continue;
            }
            // 还没有密码的历史数据：允许用手机号登录，并顺便落一个 BCrypt 密码
            if (passwordService.needsUpgrade(employee.getPassword())) {
                if (employee.getPhone() != null && employee.getPhone().equals(password)) {
                    employee.setPassword(passwordService.encode(password));
                    employeeRepository.save(employee);
                    return employee;
                }
                continue;
            }
            if (passwordService.matches(password, employee.getPassword())) {
                return employee;
            }
        }
        throw BusinessException.badRequest("用户名或密码错误");
    }

    /**
     * 修改员工密码
     */
    @Transactional
    public void changeEmployeePassword(Long employeeId, String oldPassword, String newPassword) {
        validateNewPassword(newPassword);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        if (Boolean.TRUE.equals(employee.getIsDel())) {
            throw BusinessException.notFound("员工不存在");
        }
        if (!verifyOldPassword(employee.getPassword(), employee.getPhone(), oldPassword)) {
            throw BusinessException.badRequest("原密码不正确");
        }
        employee.setPassword(passwordService.encode(newPassword));
        employeeRepository.save(employee);
        log.info("员工 [{}] 修改了登录密码", employee.getName());
    }

    /**
     * 修改管理员密码
     */
    @Transactional
    public void changeAdminPassword(Long adminId, String oldPassword, String newPassword) {
        validateNewPassword(newPassword);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> BusinessException.notFound("管理员不存在"));
        if (!verifyOldPassword(admin.getPassword(), null, oldPassword)) {
            throw BusinessException.badRequest("原密码不正确");
        }
        admin.setPassword(passwordService.encode(newPassword));
        adminRepository.save(admin);
        log.info("管理员 [{}] 修改了登录密码", admin.getUsername());
    }

    /**
     * 校验新密码规则
     */
    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw BusinessException.badRequest("新密码长度不能少于 6 位");
        }
        if (newPassword.length() > 64) {
            throw BusinessException.badRequest("新密码长度不能超过 64 位");
        }
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
