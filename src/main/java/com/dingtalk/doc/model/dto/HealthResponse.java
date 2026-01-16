package com.dingtalk.doc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 健康检查响应 DTO
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康检查响应")
public class HealthResponse {
    
    @Schema(description = "服务状态", example = "UP")
    private String status;
    
    @Schema(description = "检查时间", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    /**
     * 创建健康响应
     */
    public static HealthResponse up() {
        return new HealthResponse("UP", LocalDateTime.now());
    }
    
    /**
     * 创建不健康响应
     */
    public static HealthResponse down() {
        return new HealthResponse("DOWN", LocalDateTime.now());
    }
}
