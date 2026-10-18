package com.limiti.employee.common;

/**
 * 统一业务状态码。
 *
 * <p>约定：HTTP 状态码与 code 保持一致（500 除外，避免把内部错误暴露成 5xx 语义），
 * 前端只需判断 HTTP 状态码即可，不必逐接口判断业务 code。</p>
 *
 * @author Limiti
 */
public final class ApiCode {

    /** 成功 */
    public static final int OK = 200;

    /** 参数错误、业务规则校验不通过 */
    public static final int BAD_REQUEST = 400;

    /** 未登录或登录已过期 */
    public static final int UNAUTHORIZED = 401;

    /** 已登录但无权限 */
    public static final int FORBIDDEN = 403;

    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    /** 状态冲突（重复录入、不可流转的状态等） */
    public static final int CONFLICT = 409;

    /** 服务器内部错误 */
    public static final int INTERNAL = 500;

    private ApiCode() {
    }
}
