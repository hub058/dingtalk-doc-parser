package com.dingtalk.doc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档解析响应 DTO
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档解析响应")
public class ParseResponse {
    
    @Schema(description = "是否成功", example = "true")
    private boolean success;
    
    @Schema(description = "响应消息", example = "文档解析成功")
    private String message;
    
    @Schema(description = "生成的 Markdown 文件路径", 
            example = "/Users/username/Documents/dingtalk-docs/文档标题/文档标题.md")
    private String filePath;
    
    @Schema(description = "错误信息（失败时返回）", example = "ERROR")
    private String error;
    
    /**
     * 创建成功响应
     */
    public static ParseResponse success(String message, String filePath) {
        return new ParseResponse(true, message, filePath, null);
    }
    
    /**
     * 创建失败响应
     */
    public static ParseResponse failure(String message, String error) {
        return new ParseResponse(false, message, null, error);
    }
}
