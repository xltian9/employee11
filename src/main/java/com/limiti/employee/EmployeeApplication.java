package com.limiti.employee;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import springfox.documentation.oas.annotations.EnableOpenApi;

/**
 * 员工管理系统启动类
 * @author Limiti
 * @date 2024/11/15
 */
@EnableOpenApi
@SpringBootApplication
@EnableJpaRepositories
@Slf4j
public class EmployeeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeApplication.class, args);
        log.info("系统启动成功");
    }
}
