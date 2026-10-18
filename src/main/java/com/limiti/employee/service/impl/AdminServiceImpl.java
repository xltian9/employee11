package com.limiti.employee.service.impl;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.security.PasswordService;
import com.limiti.employee.service.AdminService;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    private final PasswordService passwordService;

    public AdminServiceImpl(AdminRepository adminRepository, PasswordService passwordService) {
        this.adminRepository = adminRepository;
        this.passwordService = passwordService;
    }

    @Override
    public Admin addAdmin(Admin admin) {
        // 管理员密码统一 BCrypt 存储
        if (admin.getPassword() != null && !admin.getPassword().trim().isEmpty()) {
            admin.setPassword(passwordService.encode(admin.getPassword().trim()));
        }
        return adminRepository.save(admin);
    }

    @Override
    public Admin updateAdmin(Admin admin) {
        // 只有传了明文/历史散列才重新加密，避免把已有 BCrypt 密文二次散列
        if (admin.getPassword() != null && passwordService.needsUpgrade(admin.getPassword())) {
            admin.setPassword(passwordService.encode(admin.getPassword().trim()));
        }
        return adminRepository.save(admin);
    }
}
