package com.dingtalk.doc.controller;

import com.dingtalk.doc.model.DocumentResult;
import com.dingtalk.doc.model.dto.HealthResponse;
import com.dingtalk.doc.model.dto.ParseRequest;
import com.dingtalk.doc.model.dto.ParseResponse;
import com.dingtalk.doc.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 文档控制器
 * 提供文档解析的 REST API 接口
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@Tag(name = "文档解析", description = "钉钉文档解析 API")
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 解析钉钉文档
     * 
     * @param request 解析请求
     * @return 解析响应
     */
    @PostMapping("/parse")
    @Operation(summary = "解析钉钉文档", description = "解析钉钉文档并生成 Markdown 文件")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "解析成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ParseResponse> parseDocument(@Valid @RequestBody ParseRequest request) {
        log.info("收到文档解析请求: {}", request.getDocumentUrl());
        
        try {
            // 调用服务解析文档
            DocumentResult result = documentService.parseDocument(
                request.getDocumentUrl(),
                request.getCookie()
            );
            
            // 构造成功响应
            ParseResponse response = ParseResponse.success(
                "文档解析成功",
                result.getFilePath()
            );
            
            log.info("文档解析成功: {}", result.getFilePath());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("文档解析失败", e);
            
            // 构造失败响应
            ParseResponse response = ParseResponse.failure(
                "文档解析失败",
                e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @ApiResponse(responseCode = "200", description = "服务正常")
    public ResponseEntity<HealthResponse> health() {
        log.debug("健康检查请求");
        return ResponseEntity.ok(HealthResponse.up());
    }
    
    /**
     * 读取 Markdown 文件内容
     * 
     * @param filePath 文件路径
     * @return Markdown 内容
     */
    @GetMapping("/markdown")
    @Operation(summary = "读取 Markdown 文件", description = "根据文件路径读取 Markdown 文件内容")
    @ApiResponse(responseCode = "200", description = "读取成功")
    public ResponseEntity<String> getMarkdown(@RequestParam String filePath) {
        log.info("读取 Markdown 文件: {}", filePath);
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            
            // 安全检查：确保文件存在且是 .md 文件
            if (!java.nio.file.Files.exists(path)) {
                log.warn("文件不存在: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("文件不存在");
            }
            
            if (!filePath.toLowerCase().endsWith(".md")) {
                log.warn("不是 Markdown 文件: {}", filePath);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("只支持读取 .md 文件");
            }
            
            // 读取文件内容
            String content = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            
            log.info("Markdown 文件读取成功，长度: {} 字符", content.length());
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(content);
            
        } catch (Exception e) {
            log.error("读取 Markdown 文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("读取文件失败: " + e.getMessage());
        }
    }
}
