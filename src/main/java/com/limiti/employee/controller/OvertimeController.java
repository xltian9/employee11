package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.OvertimeRecord;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.OvertimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 加班登记接口（仅管理员）
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/overtimes")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @GetMapping
    public Result<List<OvertimeRecord>> list(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month,
                                             HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        java.time.LocalDate now = java.time.LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        return Result.success(overtimeService.listByMonth(y, m));
    }

    @GetMapping("/employee/{employeeId}")
    public Result<List<OvertimeRecord>> listByEmployee(@PathVariable Long employeeId,
                                                       HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        java.time.LocalDate now = java.time.LocalDate.now();
        return Result.success(overtimeService.listByEmployee(employeeId, now.getYear(), now.getMonthValue()));
    }

    @PostMapping
    public Result<OvertimeRecord> create(@RequestBody OvertimeRecord record, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        record.setOperatorId(admin == null ? null : admin.getId());
        record.setOperatorName(admin == null ? "系统" : admin.getName());
        return Result.success("登记成功", overtimeService.create(record, admin == null ? "系统" : admin.getName()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        overtimeService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/types")
    public Result<java.util.Map<String, Object>> types() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("1", "工作日加班（1.5 倍）");
        map.put("2", "周末加班（2 倍）");
        map.put("3", "法定节假日加班（3 倍）");
        return Result.success(map);
    }
}
