package com.limiti.employee.security;

import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时的密码迁移任务
 *
 * <p>引入真实密码之前，员工登录只比对“姓名 + 手机号”，数据库里没有 password 列。
 * 这里给所有还没有密码的员工补一个初始密码（即其手机号）的 BCrypt 散列，
 * 让老账号可以继续登录，同时不再明文/无盐存储。任务幂等：只处理密码为空的记录。</p>
 *
 * @author Limiti
 */
@Slf4j
@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordService passwordService;

    @Override
    public void run(ApplicationArguments args) {
        List<Employee> employees = employeeRepository.findAll();
        int migrated = 0;
        for (Employee employee : employees) {
            if (employee.getPassword() != null && !employee.getPassword().trim().isEmpty()) {
                continue;
            }
            if (employee.getPhone() == null || employee.getPhone().trim().isEmpty()) {
                log.warn("员工 [{}] 没有手机号，无法生成初始密码，跳过", employee.getName());
                continue;
            }
            employee.setPassword(passwordService.encode(employee.getPhone().trim()));
            employeeRepository.save(employee);
            migrated++;
        }
        if (migrated > 0) {
            log.info("已为 {} 名员工生成初始密码（初始密码 = 手机号，BCrypt 存储）", migrated);
        }
    }
}
