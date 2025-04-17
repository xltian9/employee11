package com.limiti.employee.exception;

import com.limiti.employee.common.Result;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;

/**
 * 全局异常处理器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public Result<?> handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("数据不存在：", e);
        return Result.error(404, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数错误：", e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        logger.error("系统错误：", e);
        return Result.error(500, "系统错误，请联系管理员");
    }
} 