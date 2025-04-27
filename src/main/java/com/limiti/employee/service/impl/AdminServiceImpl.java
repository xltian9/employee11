package com.limiti.employee.service.impl;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.service.AdminService;
import com.limiti.employee.utils.WorkdayUtils;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    public AdminServiceImpl(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public Admin addAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    @Override
    public Admin updateAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    public static void main(String[] args) {
        int currentMonthWorkdays = WorkdayUtils.getCurrentMonthWorkdays();
        System.out.println(currentMonthWorkdays);
    }
}
