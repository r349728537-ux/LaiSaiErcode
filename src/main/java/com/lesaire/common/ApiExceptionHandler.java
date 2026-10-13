package com.lesaire.common;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 统一把参数校验、业务异常和上传异常转换为含 message 字段的 JSON 响应。
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * 上传超过容器配置的大小限制时返回 HTTP 413。
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", "图片不能超过 5 MB"));
    }
    /**
     * 缺少上传文件字段时返回明确的输入提示。
     */
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ResponseEntity<?> missingFile() {
        return ResponseEntity.badRequest().body(Map.of("message", "请选择要上传的图片"));
    }
    /**
     * 返回首个字段校验错误，便于前端直接展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage()).findFirst().orElse("请检查输入内容");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
    /**
     * 保留业务层指定的 HTTP 状态码和面向用户的错误信息。
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> business(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "请求无法完成" : ex.getReason()));
    }
    /**
     * 处理 JSON 格式错误或字段类型不匹配的请求。
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("message", "请求内容格式不正确")); }
    /**
     * 把数据库完整性约束冲突转换成 HTTP 409 响应。
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "此请求已处理，请刷新列表后重试")); }
}
