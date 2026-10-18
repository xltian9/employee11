package com.limiti.employee.security;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 从 HttpSession 中读取当前登录者，并提供越权判定的统一入口。
 *
 * <p>约定：管理员登录写入 session 的 "admin"，员工登录写入 "employee"（沿用原有键名）。</p>
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
}
