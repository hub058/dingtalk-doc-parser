package com.dingtalk.doc.exception;

/**
 * HTTP 请求异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class HttpRequestException extends RuntimeException {
    
    public HttpRequestException(String message) {
        super(message);
    }
    
    public HttpRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
