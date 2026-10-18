package com.limiti.employee.service;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.AuditLog;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.AuditLogRepository;
import com.limiti.employee.security.SessionUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 审计日志服务。
 *
 * <p>设计取舍：没有用 AOP 注解切面，而是在业务代码里显式调用。
 * 原因是审计最需要的是"改成了什么"（如 30000 → 35000），这只能由业务层提供，
 * 切面拿不到变更前后的值，硬写只会得到一堆无信息量的"调用了某方法"。</p>
 *
 * @author Limiti
 */
@Slf4j
@Service
public class AuditLogService {

    /** 动作：工资调整（补发/扣款） */
    public static final String SALARY_ADJUST = "SALARY_ADJUST";
    /** 动作：员工提交请假 */
    public static final String LEAVE_APPLY = "LEAVE_APPLY";
    /** 动作：请假审批 */
    public static final String LEAVE_APPROVE = "LEAVE_APPROVE";
    /** 动作：请假撤销 */
    public static final String LEAVE_CANCEL = "LEAVE_CANCEL";
    /** 动作：绩效录入/修改 */
    public static final String PERFORMANCE_SAVE = "PERFORMANCE_SAVE";
    /** 动作：新增员工 */
    public static final String EMPLOYEE_CREATE = "EMPLOYEE_CREATE";
    /** 动作：员工信息变更（含工资） */
    public static final String EMPLOYEE_UPDATE = "EMPLOYEE_UPDATE";
    /** 动作：删除员工 */
    public static final String EMPLOYEE_DELETE = "EMPLOYEE_DELETE";
    /** 动作：薪酬规则变更 */
    public static final String POLICY_UPDATE = "POLICY_UPDATE";
    /** 动作：加班录入 */
    public static final String OVERTIME_SAVE = "OVERTIME_SAVE";
    /** 动作：登录 */
    public static final String LOGIN = "LOGIN";
    /** 动作：修改密码 */
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 记录一条审计日志（操作人从当前请求的 session 中取）
     */
    public void record(String action, String actionName, String targetType,
                       Long targetId, String targetName, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setActionName(actionName);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setTargetName(targetName);
            entry.setDetail(detail);
            entry.setCreateTime(new Date());

            HttpServletRequest request = currentRequest();
            if (request == null) {
                entry.setOperatorName("系统");
                entry.setOperatorType("SYSTEM");
            } else {
                Admin admin = SessionUser.admin(request);
                Employee employee = SessionUser.employee(request);
                if (admin != null) {
                    entry.setOperatorId(admin.getId());
                    entry.setOperatorName(admin.getName() == null ? admin.getUsername() : admin.getName());
                    entry.setOperatorType("ADMIN");
                } else if (employee != null) {
                    entry.setOperatorId(employee.getId());
                    entry.setOperatorName(employee.getName());
                    entry.setOperatorType("EMPLOYEE");
                } else {
                    entry.setOperatorName("匿名");
                    entry.setOperatorType("ANONYMOUS");
                }
                entry.setIp(clientIp(request));
            }
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // 审计失败不能影响主业务
            log.error("写入审计日志失败, action={}, target={}", action, targetName, e);
        }
    }

    public Page<AuditLog> search(String operator, String action, String targetType,
                                 Date from, Date to, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        return auditLogRepository.search(blankToNull(operator), blankToNull(action),
                blankToNull(targetType), from, to, pageable);
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return null;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
