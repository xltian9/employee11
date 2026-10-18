package com.limiti.employee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limiti.employee.common.Result;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 接口鉴权拦截器
 *
 * <p>挂在 /api/** 上：除登录、登出外，所有接口都必须在 session 中存在当前登录者，
 * 否则直接返回 401 JSON，不再把请求交给 Controller。</p>
 *
 * <p>注意：这里只解决“有没有登录”。至于“能不能动别人的数据”，
 * 由各 Controller 用 {@link SessionUser#canAccessEmployee} 做归属校验。</p>
 *
 * @author Limiti
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 免登录接口（精确匹配，避免被 /api/auth/loginXxx 之类的路径绕过） */
    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/logout"
    ));

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        // 只保护 /api/**，静态资源交给资源处理器
        if (!uri.startsWith("/api/") || WHITELIST.contains(uri)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null
                && (session.getAttribute(SessionUser.ADMIN_KEY) != null
                || session.getAttribute(SessionUser.EMPLOYEE_KEY) != null);

        if (loggedIn) {
            return true;
        }

        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                Result.error(401, "未登录或登录已过期，请重新登录"));
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, Result<?> body) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
