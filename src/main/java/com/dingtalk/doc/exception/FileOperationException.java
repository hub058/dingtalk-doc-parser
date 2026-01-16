package com.dingtalk.doc.exception;

/**
 * 文件操作异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class FileOperationException extends RuntimeException {
    
    public FileOperationException(String message) {
        super(message);
    }
    
    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
