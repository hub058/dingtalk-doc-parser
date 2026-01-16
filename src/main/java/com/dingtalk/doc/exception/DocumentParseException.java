package com.dingtalk.doc.exception;

/**
 * 文档解析异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class DocumentParseException extends RuntimeException {
    
    public DocumentParseException(String message) {
        super(message);
    }
    
    public DocumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
