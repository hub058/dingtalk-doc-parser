package com.dingtalk.doc.controller;

import com.dingtalk.doc.model.DingTalkUser;
import com.dingtalk.doc.model.DocNode;
import com.dingtalk.doc.model.Workspace;
import com.dingtalk.doc.service.FileManager;
import com.dingtalk.doc.service.MarkdownGenerator;
import com.dingtalk.doc.service.api.DingTalkAuthService;
import com.dingtalk.doc.service.api.DingTalkUserService;
import com.dingtalk.doc.service.api.DingTalkWorkspaceService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉开放平台 API 控制器
 * 
 * @author DingTalk Doc Parser Team
 */
@RestController
@RequestMapping("/api/dingtalk")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "dingtalk.api.enabled", havingValue = "true")
public class DingTalkApiController {
    
    private final DingTalkAuthService authService;
    private final DingTalkWorkspaceService workspaceService;
    private final DingTalkUserService userService;
    private final MarkdownGenerator markdownGenerator;
    private final FileManager fileManager;
    
    /**
     * 1. 获取 AccessToken
     * 
     * GET /api/dingtalk/token
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getAccessToken() {
        try {
            String token = authService.getAccessToken();
            long remainingTime = authService.getTokenRemainingTime();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("accessToken", token);
            result.put("remainingTime", remainingTime);
            result.put("message", "AccessToken 获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取 AccessToken 失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取 AccessToken 失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 2. 获取知识库列表
     * 
     * GET /api/dingtalk/workspaces
     */
    @GetMapping("/workspaces")
    public ResponseEntity<Map<String, Object>> getWorkspaces() {
        try {
            List<Workspace> workspaces = workspaceService.getWorkspaces();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", workspaces.size());
            result.put("workspaces", workspaces);
            result.put("message", "知识库列表获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取知识库列表失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 3. 获取指定知识库的节点列表
     * 
     * GET /api/dingtalk/workspaces/{workspaceId}/nodes
     */
    @GetMapping("/workspaces/{workspaceId}/nodes")
    public ResponseEntity<Map<String, Object>> getNodes(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String parentNodeId) {
        try {
            List<DocNode> nodes = workspaceService.getNodes(workspaceId, parentNodeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workspaceId", workspaceId);
            result.put("parentNodeId", parentNodeId);
            result.put("count", nodes.size());
            result.put("nodes", nodes);
            result.put("message", "节点列表获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取节点列表失败: workspaceId={}", workspaceId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取节点列表失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 4. 获取文档内容
     * 
     * GET /api/dingtalk/workspaces/{workspaceId}/nodes/{nodeId}/content
     */
    @GetMapping("/workspaces/{workspaceId}/nodes/{nodeId}/content")
    public ResponseEntity<Map<String, Object>> getDocumentContent(
            @PathVariable String workspaceId,
            @PathVariable String nodeId) {
        try {
            JsonNode content = workspaceService.getDocumentContent(workspaceId, nodeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workspaceId", workspaceId);
            result.put("nodeId", nodeId);
            result.put("content", content);
            result.put("message", "文档内容获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取文档内容失败: nodeId={}", nodeId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取文档内容失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 5. 解析文档并生成 Markdown
     * 
     * POST /api/dingtalk/workspaces/{workspaceId}/nodes/{nodeId}/parse
     */
    @PostMapping("/workspaces/{workspaceId}/nodes/{nodeId}/parse")
    public ResponseEntity<Map<String, Object>> parseDocument(
            @PathVariable String workspaceId,
            @PathVariable String nodeId) {
        try {
            // 1. 获取节点信息
            DocNode node = workspaceService.getNode(workspaceId, nodeId);
            
            // 2. 获取文档内容
            JsonNode content = workspaceService.getDocumentContent(workspaceId, nodeId);
            
            // 3. 准备输出目录
            String outputDir = fileManager.prepareOutputDirectory(node.getName());
            
            // 4. 生成 Markdown
            String markdown = markdownGenerator.generateMarkdown(
                content,
                node.getName(),
                null,  // 使用 API 不需要 Cookie
                outputDir
            );
            
            // 5. 保存文件
            String filePath = fileManager.saveMarkdownFile(node.getName(), markdown);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workspaceId", workspaceId);
            result.put("nodeId", nodeId);
            result.put("nodeName", node.getName());
            result.put("filePath", filePath);
            result.put("markdownLength", markdown.length());
            result.put("message", "文档解析成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("解析文档失败: nodeId={}", nodeId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "解析文档失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 6. 批量解析知识库中的所有文档
     * 
     * POST /api/dingtalk/workspaces/{workspaceId}/parse-all
     */
    @PostMapping("/workspaces/{workspaceId}/parse-all")
    public ResponseEntity<Map<String, Object>> parseAllDocuments(
            @PathVariable String workspaceId) {
        try {
            // 1. 获取所有节点
            List<DocNode> allNodes = workspaceService.getAllNodesRecursively(workspaceId, null);
            
            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;
            
            // 2. 遍历所有节点，只处理文件类型
            for (DocNode node : allNodes) {
                if (!"file".equals(node.getType())) {
                    skippedCount++;
                    continue;
                }
                
                try {
                    // 获取文档内容
                    JsonNode content = workspaceService.getDocumentContent(workspaceId, node.getNodeId());
                    
                    // 准备输出目录
                    String outputDir = fileManager.prepareOutputDirectory(node.getName());
                    
                    // 生成 Markdown
                    String markdown = markdownGenerator.generateMarkdown(
                        content,
                        node.getName(),
                        null,
                        outputDir
                    );
                    
                    // 保存文件
                    fileManager.saveMarkdownFile(node.getName(), markdown);
                    
                    successCount++;
                    log.info("成功解析文档: {}", node.getName());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("解析文档失败: {}", node.getName(), e);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workspaceId", workspaceId);
            result.put("totalNodes", allNodes.size());
            result.put("successCount", successCount);
            result.put("failureCount", failureCount);
            result.put("skippedCount", skippedCount);
            result.put("message", String.format(
                "批量解析完成: 成功 %d 个，失败 %d 个，跳过 %d 个",
                successCount, failureCount, skippedCount
            ));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量解析文档失败: workspaceId={}", workspaceId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "批量解析文档失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 7. 强制刷新 AccessToken
     * 
     * POST /api/dingtalk/token/refresh
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken() {
        try {
            String token = authService.forceRefresh();
            long remainingTime = authService.getTokenRemainingTime();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("accessToken", token);
            result.put("remainingTime", remainingTime);
            result.put("message", "AccessToken 刷新成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("刷新 AccessToken 失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "刷新 AccessToken 失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 8. 根据 userId 获取用户详情
     * 
     * GET /api/dingtalk/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserByUserId(@PathVariable String userId) {
        try {
            DingTalkUser user = userService.getUserByUserId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", user);
            result.put("message", "获取用户详情成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取用户详情失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 9. 根据 userId 获取用户的 unionid
     * 
     * GET /api/dingtalk/users/{userId}/unionid
     */
    @GetMapping("/users/{userId}/unionid")
    public ResponseEntity<Map<String, Object>> getUserUnionId(@PathVariable String userId) {
        try {
            String unionid = userService.getUserUnionId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("unionid", unionid);
            result.put("message", "获取 unionid 成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取 unionid 失败: userId={}", userId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取 unionid 失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 10. 根据手机号获取用户详情
     * 
     * POST /api/dingtalk/users/getByMobile
     */
    @PostMapping("/users/getByMobile")
    public ResponseEntity<Map<String, Object>> getUserByMobile(@RequestBody Map<String, String> request) {
        try {
            String mobile = request.get("mobile");
            
            if (mobile == null || mobile.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            String user = userService.getUserByMobile(mobile);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("user", user);
            result.put("message", "获取用户详情成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("根据手机号获取用户详情失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "根据手机号获取用户详情失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 11. 根据手机号获取用户的 unionid
     * 
     * POST /api/dingtalk/users/getUnionIdByMobile
     */
    @PostMapping("/users/getUnionIdByMobile")
    public ResponseEntity<Map<String, Object>> getUserUnionIdByMobile(@RequestBody Map<String, String> request) {
        try {
            String mobile = request.get("mobile");
            
            if (mobile == null || mobile.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            String userId = userService.getUserByMobile(mobile);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("mobile", mobile);
            result.put("userId", userId);
            result.put("message", "获取 userId 成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("根据手机号获取 userId 失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "根据手机号获取 unionid 失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }
}
