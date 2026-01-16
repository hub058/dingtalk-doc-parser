package com.dingtalk.doc.exception;

/**
 * Cookie 不存在异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class CookieNotFoundException extends RuntimeException {
    
    public CookieNotFoundException(String message) {
        super(message);
    }
    
    public CookieNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
