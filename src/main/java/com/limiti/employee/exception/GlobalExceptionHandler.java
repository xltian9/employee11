package com.limiti.employee.exception;

import com.limiti.employee.common.ApiCode;
import com.limiti.employee.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器：系统中所有可预期的失败都从这里统一出口。
 *
 * <p>约定：HTTP 状态码 = 业务 code，响应体固定为 {@link Result} 结构，
 * 因此前端只需要一套拦截逻辑（非 2xx → 取出 msg 提示；401 → 跳登录）。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：权限、状态冲突、参数校验等
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务校验失败 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(e.getCode(), e.getMessage());
    }

    /**
     * 资源不存在
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Result<?>> handleNotFound(Exception e, HttpServletRequest request) {
        log.warn("数据不存在 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(ApiCode.NOT_FOUND, "请求的数据不存在或已被删除");
    }

    /**
     * 参数错误
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Result<?>> handleBadRequest(RuntimeException e, HttpServletRequest request) {
        log.warn("参数错误 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(ApiCode.BAD_REQUEST, e.getMessage() == null ? "参数不正确" : e.getMessage());
    }

    /**
     * 请求体 JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return build(ApiCode.BAD_REQUEST, "请求内容格式不正确，请检查必填字段");
    }

    /**
     * 必填参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<?>> handleMissingParam(MissingServletRequestParameterException e) {
        return build(ApiCode.BAD_REQUEST, "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(ApiCode.BAD_REQUEST, "参数格式不正确：" + e.getName());
    }

    /**
     * 实体字段校验失败（@Valid + @NotNull 等）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<?>> handleValidation(Exception e) {
        String message = "提交的内容不合要求";
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                message = ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (e instanceof ConstraintViolationException) {
            message = ((ConstraintViolationException) e).getMessage();
        }
        return build(ApiCode.BAD_REQUEST, message);
    }

    /**
     * 数据库约束冲突：唯一键、外键
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<?>> handleDataIntegrity(DataIntegrityViolationException e) {
        String raw = String.valueOf(e.getMostSpecificCause().getMessage());
        String message = "数据保存失败";
        if (raw.contains("Duplicate entry")) {
            message = "数据重复：该记录已存在";
        } else if (raw.contains("foreign key constraint")) {
            message = "存在关联数据，无法完成该操作";
        } else if (raw.contains("cannot be null")) {
            message = "必填字段不能为空";
        }
        log.warn("数据库约束冲突: {}", raw);
        return build(ApiCode.CONFLICT, message);
    }

    /**
     * 上传文件超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<?>> handleUploadSize(MaxUploadSizeExceededException e) {
        return build(ApiCode.BAD_REQUEST, "上传文件过大，单个文件不能超过 5MB");
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(ApiCode.BAD_REQUEST, "不支持的请求方式：" + e.getMethod());
    }

    /**
     * 兜底：未预期的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统错误 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return build(ApiCode.INTERNAL, "系统繁忙，请稍后重试或联系管理员");
    }

    private ResponseEntity<Result<?>> build(int code, String message) {
        int httpCode = code == ApiCode.OK ? 200 : code;
        HttpStatus status = HttpStatus.resolve(httpCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(Result.error(code, message));
    }
}
