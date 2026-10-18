package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PageResult;
import com.limiti.employee.entity.AuditLog;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志查询接口（仅管理员）
 *
 * <p>回答"谁改了谁的工资、谁批了谁的假"：每条日志都有操作人、动作、目标对象、
 * 变更明细与 IP。</p>
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public Result<PageResult<AuditLog>> list(@RequestParam(required = false) String operator,
                                             @RequestParam(required = false) String action,
                                             @RequestParam(required = false) String targetType,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        Date from = parseDate(startDate, false);
        Date to = parseDate(endDate, true);
        return Result.success(PageResult.of(auditLogService.search(operator, action, targetType, from, to, page, size)));
    }

    /**
     * 动作字典，供前端筛选下拉使用
     */
    @GetMapping("/actions")
    public Result<Map<String, String>> actions(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        Map<String, String> map = new LinkedHashMap<>();
        map.put(AuditLogService.LOGIN, "登录");
        map.put(AuditLogService.PASSWORD_CHANGE, "修改密码");
        map.put(AuditLogService.LEAVE_APPLY, "提交请假");
        map.put(AuditLogService.LEAVE_APPROVE, "审批请假");
        map.put(AuditLogService.LEAVE_CANCEL, "撤销请假");
        map.put(AuditLogService.PERFORMANCE_SAVE, "录入绩效");
        map.put(AuditLogService.SALARY_ADJUST, "工资调整");
        map.put(AuditLogService.POLICY_UPDATE, "修改薪酬规则");
        map.put(AuditLogService.OVERTIME_SAVE, "加班登记");
        map.put(AuditLogService.EMPLOYEE_CREATE, "新增员工");
        map.put(AuditLogService.EMPLOYEE_UPDATE, "修改员工");
        map.put(AuditLogService.EMPLOYEE_DELETE, "删除员工");
        return Result.success(map);
    }

    private Date parseDate(String text, boolean endOfDay) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(endOfDay ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm:ss");
            Date parsed = format.parse(text.trim() + (endOfDay ? " 23:59:59" : " 00:00:00"));
            return parsed;
        } catch (ParseException e) {
            throw new IllegalArgumentException("日期格式必须是 yyyy-MM-dd");
        }
    }
}
