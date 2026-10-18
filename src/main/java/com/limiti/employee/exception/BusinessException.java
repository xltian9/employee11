package com.limiti.employee.exception;

import com.limiti.employee.common.ApiCode;

/**
 * 业务异常：把「参数不对 / 没权限 / 状态冲突」这类可预期的失败，从控制器的 try-catch 里
 * 搬到全局异常处理器统一转换成 {@code Result} 与对应的 HTTP 状态码。
 *
 * <p>用法：{@code throw new BusinessException(ApiCode.CONFLICT, "该季度业绩已录入");}</p>
 *
 * @author Limiti
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(ApiCode.BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ApiCode.BAD_REQUEST, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ApiCode.FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ApiCode.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ApiCode.CONFLICT, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ApiCode.UNAUTHORIZED, message);
    }
}
