package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Api(tags = "管理员接口")
public class AdminController {
    
    @GetMapping("/info")
    @ApiOperation("获取管理员信息")
    public Result<Admin> getInfo() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setStatus(true);
        return Result.success(admin);
    }
} 