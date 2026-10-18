package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Department;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 部门管理控制器
 *
 * <p>异常统一抛给全局处理器，不再逐个方法 try/catch。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@RestController
@RequestMapping("/api/departments")
@Api(tags = "部门管理接口")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取所有部门（登录即可，员工页面下拉框会用到）
     */
    @GetMapping
    @ApiOperation("获取所有部门")
    public Result<List<Department>> getAllDepartments() {
        return Result.success(departmentService.findAll());
    }

    /**
     * 创建部门（仅管理员）
     */
    @PostMapping
    @ApiOperation("创建部门")
    public Result<Department> createDepartment(@RequestBody Department department,
                                               HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success("创建成功", departmentService.save(department));
    }

    /**
     * 更新部门（仅管理员）
     */
    @PutMapping("/{id}")
    @ApiOperation("更新部门")
    public Result<Department> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department,
            HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success("修改成功", departmentService.update(id, department));
    }

    /**
     * 删除部门（仅管理员）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        departmentService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 搜索部门（登录即可）
     */
    @GetMapping("/search")
    @ApiOperation("搜索部门")
    public Result<List<Department>> searchDepartments(@RequestParam String keyword) {
        return Result.success(departmentService.searchDepartments(keyword));
    }

    /**
     * 根据ID获取部门（登录即可）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取部门")
    public Result<Department> getDepartment(@PathVariable Long id) {
        Department department = departmentService.findById(id);
        if (department == null) {
            throw BusinessException.notFound("部门不存在");
        }
        return Result.success(department);
    }
}
