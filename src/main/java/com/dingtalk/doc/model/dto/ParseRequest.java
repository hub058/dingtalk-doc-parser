package com.dingtalk.doc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档解析请求 DTO
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档解析请求")
public class ParseRequest {
    
    @NotBlank(message = "文档 URL 不能为空")
    @Schema(description = "钉钉文档 URL 或 Node ID", 
            example = "https://alidocs.dingtalk.com/i/nodes/xxx",
            required = true)
    private String documentUrl;
    
    @Schema(description = "钉钉登录 Cookie（可选，未提供则使用环境变量）",
            example = "cookie_value_here")
    private String cookie;
}
