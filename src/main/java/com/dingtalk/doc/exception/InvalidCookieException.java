package com.dingtalk.doc.exception;

/**
 * Cookie 无效异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class InvalidCookieException extends RuntimeException {
    
    public InvalidCookieException(String message) {
        super(message);
    }
    
    public InvalidCookieException(String message, Throwable cause) {
        super(message, cause);
    }
}
