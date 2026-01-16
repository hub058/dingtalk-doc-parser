package com.dingtalk.doc.exception;

import com.dingtalk.doc.model.dto.ParseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理各种异常并返回标准响应格式
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理 Cookie 不存在异常
     */
    @ExceptionHandler(CookieNotFoundException.class)
    public ResponseEntity<ParseResponse> handleCookieNotFound(CookieNotFoundException ex) {
        log.error("Cookie 不存在: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "Cookie 不存在",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理 Cookie 无效异常
     */
    @ExceptionHandler(InvalidCookieException.class)
    public ResponseEntity<ParseResponse> handleInvalidCookie(InvalidCookieException ex) {
        log.error("Cookie 无效: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "Cookie 无效",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理 Node ID 提取异常
     */
    @ExceptionHandler(NodeIdExtractionException.class)
    public ResponseEntity<ParseResponse> handleNodeIdExtraction(NodeIdExtractionException ex) {
        log.error("Node ID 提取失败: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "无法从 URL 中提取 Node ID",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理文档解析异常
     */
    @ExceptionHandler(DocumentParseException.class)
    public ResponseEntity<ParseResponse> handleDocumentParse(DocumentParseException ex) {
        log.error("文档解析失败: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "文档解析失败",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理 HTTP 请求异常
     */
    @ExceptionHandler(HttpRequestException.class)
    public ResponseEntity<ParseResponse> handleHttpRequest(HttpRequestException ex) {
        log.error("HTTP 请求失败: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "HTTP 请求失败",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理文件操作异常
     */
    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<ParseResponse> handleFileOperation(FileOperationException ex) {
        log.error("文件操作失败: {}", ex.getMessage());
        
        ParseResponse response = ParseResponse.failure(
            "文件操作失败",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ParseResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("参数验证失败: {}", errors);
        
        ParseResponse response = ParseResponse.failure(
            "请求参数验证失败",
            errors.toString()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ParseResponse> handleGeneral(Exception ex) {
        log.error("系统错误", ex);
        
        ParseResponse response = ParseResponse.failure(
            "系统错误",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
