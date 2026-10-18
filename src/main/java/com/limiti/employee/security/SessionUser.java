package com.limiti.employee.security;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.exception.BusinessException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 从 HttpSession 中读取当前登录者，并提供越权判定的统一入口。
 *
 * <p>约定：管理员登录写入 session 的 "admin"，员工登录写入 "employee"（沿用原有键名）。</p>
 *
 * <p>requireXxx 系列方法在无权限时抛 {@link BusinessException}，
 * 由全局异常处理器统一转成 401/403，控制器里不再需要 if + return。</p>
 *
 * @author Limiti
 */
public final class SessionUser {

    public static final String ADMIN_KEY = "admin";
    public static final String EMPLOYEE_KEY = "employee";

    private SessionUser() {
    }

    /**
     * 当前登录的管理员，未登录返回 null
     */
    public static Admin admin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ADMIN_KEY);
        return value instanceof Admin ? (Admin) value : null;
    }

    /**
     * 当前登录的员工，未登录返回 null
     */
    public static Employee employee(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(EMPLOYEE_KEY);
        return value instanceof Employee ? (Employee) value : null;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return admin(request) != null;
    }

    public static boolean isEmployee(HttpServletRequest request) {
        return employee(request) != null;
    }

    /**
     * 是否允许访问指定员工的数据：管理员放行；员工只允许访问自己的。
     */
    public static boolean canAccessEmployee(HttpServletRequest request, Long employeeId) {
        if (isAdmin(request)) {
            return true;
        }
        Employee current = employee(request);
        return current != null && employeeId != null && employeeId.equals(current.getId());
    }

    /**
     * 要求当前请求来自管理员，否则抛 403
     */
    public static Admin requireAdmin(HttpServletRequest request) {
        Admin admin = admin(request);
        if (admin == null) {
            if (isEmployee(request)) {
                throw BusinessException.forbidden("该操作仅管理员可以执行");
            }
            throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
        }
        return admin;
    }

    /**
     * 要求当前请求来自员工本人，否则抛 403
     */
    public static Employee requireSelf(HttpServletRequest request) {
        Employee employee = employee(request);
        if (employee == null) {
            throw BusinessException.forbidden("请使用员工账号登录后操作");
        }
        return employee;
    }

    /**
     * 要求当前请求能访问指定员工的数据（管理员或本人）
     */
    public static void requireAccessEmployee(HttpServletRequest request, Long employeeId) {
        if (!canAccessEmployee(request, employeeId)) {
            throw BusinessException.forbidden("无权访问其他员工的数据");
        }
    }

    /**
     * 当前登录员工的 ID；管理员或未登录返回 null
     */
    public static Long currentEmployeeId(HttpServletRequest request) {
        Employee employee = employee(request);
        return employee == null ? null : employee.getId();
    }

    /**
     * 当前登录者的显示名，用于审计日志；未登录返回"匿名"
     */
    public static String currentName(HttpServletRequest request) {
        Admin admin = admin(request);
        if (admin != null) {
            return admin.getName() == null ? admin.getUsername() : admin.getName();
        }
        Employee employee = employee(request);
        if (employee != null) {
            return employee.getName();
        }
        return "匿名";
    }
}
