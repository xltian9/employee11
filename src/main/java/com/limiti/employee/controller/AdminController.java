package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.service.AdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

    private final AdminService adminService;

    // 构造函数注入 AdminService
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/info")
    @ApiOperation("获取管理员信息")
    public Result<Admin> getInfo() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setStatus(true);
        return Result.success(admin);
    }

    @PostMapping("/add")
    @ApiOperation("添加管理员")
    public Result<Admin> addAdmin(@Valid @RequestBody Admin admin) {
        Admin addedAdmin = adminService.addAdmin(admin);
        return Result.success(addedAdmin);
    }

    @PutMapping("/update")
    @ApiOperation("修改管理员信息")
    public Result<Admin> updateAdmin(@Valid @RequestBody Admin admin) {
        Admin updatedAdmin = adminService.updateAdmin(admin);
        return Result.success(updatedAdmin);
    }
} 