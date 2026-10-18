package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 管理员控制器
 *
 * @author Limiti
 * @date 2024/11/15
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "管理员接口")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/info")
    @ApiOperation("获取管理员信息")
    public Result<Admin> getInfo(HttpServletRequest request) {
        Admin current = SessionUser.requireAdmin(request);
        Admin admin = new Admin();
        admin.setUsername(current.getUsername());
        admin.setName(current.getName());
        admin.setPhone(current.getPhone());
        admin.setEmail(current.getEmail());
        admin.setStatus(current.getStatus());
        return Result.success(admin);
    }

    @PostMapping("/add")
    @ApiOperation("添加管理员")
    public Result<Admin> addAdmin(@Valid @RequestBody Admin admin, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            throw BusinessException.badRequest("请输入管理员用户名");
        }
        return Result.success("添加成功", adminService.addAdmin(admin));
    }

    @PutMapping("/update")
    @ApiOperation("修改管理员信息")
    public Result<Admin> updateAdmin(@Valid @RequestBody Admin admin, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        if (admin.getId() == null) {
            throw BusinessException.badRequest("缺少管理员 ID");
        }
        return Result.success("修改成功", adminService.updateAdmin(admin));
    }
}
