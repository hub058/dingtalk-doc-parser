package com.dingtalk.doc.exception;

/**
 * Node ID 提取异常
 * 
 * @author DingTalk Doc Parser Team
 */
public class NodeIdExtractionException extends RuntimeException {
    
    public NodeIdExtractionException(String message) {
        super(message);
    }
    
    public NodeIdExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
