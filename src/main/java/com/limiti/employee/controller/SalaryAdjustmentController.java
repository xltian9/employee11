package com.limiti.employee.controller;

import com.limiti.employee.common.ApiCode;
import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.SalaryAdjustment;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.SalaryAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 补发 / 扣款接口（仅管理员）
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/salary-adjustments")
public class SalaryAdjustmentController {

    @Autowired
    private SalaryAdjustmentService salaryAdjustmentService;

    @GetMapping
    public Result<List<SalaryAdjustment>> list(@RequestParam(required = false) Integer year,
                                               @RequestParam(required = false) Integer month,
                                               HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        java.time.LocalDate now = java.time.LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        return Result.success(salaryAdjustmentService.listByMonth(y, m));
    }

    @GetMapping("/employee/{employeeId}")
    public Result<List<SalaryAdjustment>> listByEmployee(@PathVariable Long employeeId,
                                                         HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(salaryAdjustmentService.listByEmployee(employeeId));
    }

    @PostMapping
    public Result<SalaryAdjustment> create(@RequestBody SalaryAdjustment adjustment,
                                           HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        adjustment.setOperatorId(admin == null ? null : admin.getId());
        adjustment.setOperatorName(admin == null ? "系统" : admin.getName());
        return Result.success("登记成功", salaryAdjustmentService.create(adjustment, admin == null ? "系统" : admin.getName()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        salaryAdjustmentService.delete(id, admin == null ? "系统" : admin.getName());
        return Result.success("删除成功", null);
    }

    @GetMapping("/types")
    public Result<java.util.Map<String, Object>> types() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("1", "补发");
        map.put("2", "扣款");
        return Result.success(map);
    }
}
