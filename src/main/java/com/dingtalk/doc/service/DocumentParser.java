package com.dingtalk.doc.service;

import com.dingtalk.doc.model.DocumentData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档解析器
 * 负责从钉钉 API 获取文档数据并解析
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParser {
    
    private final DingTalkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${dingtalk.api.api-doc-base-url}")
    private String baseUrl;
    
    @Value("${dingtalk.api.document-data-url}")
    private String documentDataUrl;
    
    /**
     * 获取完整的文档数据
     * 
     * @param urlOrNodeId 文档 URL 或 Node ID
     * @param cookie Cookie 字符串
     * @return 文档数据
     */
    public DocumentData getCompleteDocumentData(String urlOrNodeId, String cookie) {
        log.info("开始解析文档: {}", urlOrNodeId);
        
        // 步骤1: 提取 Node ID
        String nodeId = extractNodeId(urlOrNodeId);
        log.info("Node ID: {}", nodeId);
        
        // 步骤2: GET 请求获取文档页面 HTML
        log.info("正在获取文档页面...");
        String html = fetchNodeByGet(nodeId, cookie);
        log.debug("页面 HTML 长度: {} 字符", html.length());
        
        // 调试：保存 HTML 到文件
        try {
            java.nio.file.Path debugPath = java.nio.file.Paths.get(System.getProperty("user.home"), 
                "Documents", "dingtalk-docs", "debug");
            java.nio.file.Files.createDirectories(debugPath);
            java.nio.file.Path htmlFile = debugPath.resolve("page_response.html");
            java.nio.file.Files.write(htmlFile, html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("调试：HTML 已保存到 {}", htmlFile);
            
            // 打印 HTML 前 500 字符用于调试
            log.debug("HTML 前 500 字符: {}", html.substring(0, Math.min(500, html.length())));
        } catch (Exception e) {
            log.warn("保存调试 HTML 失败", e);
        }
        
        // 步骤3: 从 HTML 中提取 mainsite_server_content JSON
        log.info("正在提取文档信息...");
        JsonNode mainsiteContent = extractMainsiteContent(html);
        
        // 步骤4: 提取 Dentry Key
        String dentryKey = extractDentryKey(mainsiteContent);
        log.info("Dentry Key: {}", dentryKey);
        
        // 步骤5: 提取文档标题
        String title = extractDocumentTitle(mainsiteContent);
        log.info("文档标题: {}", title);
        
        // 步骤6: POST 请求获取文档详细数据
        log.info("正在获取文档内容...");
        JsonNode documentContent = fetchDocumentData(cookie, dentryKey);
        
        // 步骤7: 提取文档内容
        JsonNode content = extractDocumentContent(documentContent);
        
        if (content == null) {
            log.warn("无法提取文档内容（可能是 OSS 加密）");
        } else {
            log.info("文档内容提取成功");
        }
        
        return DocumentData.builder()
                .nodeId(nodeId)
                .dentryKey(dentryKey)
                .title(title)
                .mainsiteContent(mainsiteContent)
                .documentContent(documentContent)
                .content(content)
                .build();
    }
    
    /**
     * 从 URL 中提取 Node ID
     * 如果输入已经是 Node ID，则直接返回
     * 
     * @param urlOrNodeId URL 或 Node ID
     * @return Node ID
     */
    public String extractNodeId(String urlOrNodeId) {
        if (urlOrNodeId.startsWith("http")) {
            Pattern pattern = Pattern.compile("/i/nodes/([^?/]+)");
            Matcher matcher = pattern.matcher(urlOrNodeId);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            throw new RuntimeException("无法从 URL 中提取 Node ID: " + urlOrNodeId);
        }
        
        return urlOrNodeId;
    }
    
    /**
     * 通过 GET 请求获取文档页面 HTML
     * 
     * @param nodeId Node ID
     * @param cookie Cookie 字符串
     * @return HTML 内容
     */
    private String fetchNodeByGet(String nodeId, String cookie) {
        String url = baseUrl + "/i/nodes/" + nodeId + "?rnd=" + Math.random();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Cookie", cookie);
        headers.put("Referer", baseUrl);
        
        return httpClient.get(url, headers);
    }
    
    /**
     * 从 HTML 中提取 mainsite_server_content JSON 数据
     * 
     * @param html HTML 内容
     * @return JSON 数据
     */
    private JsonNode extractMainsiteContent(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Element scriptElement = doc.getElementById("mainsite_server_content");
            
            if (scriptElement == null) {
                // 调试：查找所有 script 标签
                log.error("未找到 mainsite_server_content 元素");
                log.debug("HTML 中的所有 script 标签 ID:");
                doc.select("script[id]").forEach(script -> {
                    log.debug("  - ID: {}", script.id());
                });
                
                // 尝试查找包含 "mainsite" 的 script 标签
                Element mainsiteScript = doc.select("script[id*=mainsite]").first();
                if (mainsiteScript != null) {
                    log.info("找到类似的 script 标签: {}", mainsiteScript.id());
                    String jsonContent = mainsiteScript.html().trim();
                    return objectMapper.readTree(jsonContent);
                }
                
                throw new RuntimeException("未找到 mainsite_server_content 元素，也未找到类似的 script 标签");
            }
            
            String jsonContent = scriptElement.html().trim();
            log.debug("提取的 JSON 内容长度: {} 字符", jsonContent.length());
            log.debug("JSON 内容前 200 字符: {}", jsonContent.substring(0, Math.min(200, jsonContent.length())));
            
            return objectMapper.readTree(jsonContent);
        } catch (Exception e) {
            log.error("提取 mainsite_server_content 失败", e);
            throw new RuntimeException("提取 mainsite_server_content 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 mainsite_content 中提取 Dentry Key
     * 
     * @param mainsiteContent mainsite content JSON
     * @return Dentry Key
     */
    private String extractDentryKey(JsonNode mainsiteContent) {
        // 尝试从 dentryInfo.data.dentryKey 获取
        JsonNode dentryInfo = mainsiteContent.path("dentryInfo");
        if (!dentryInfo.isMissingNode()) {
            JsonNode data = dentryInfo.path("data");
            if (!data.isMissingNode()) {
                JsonNode dentryKey = data.path("dentryKey");
                if (!dentryKey.isMissingNode()) {
                    return dentryKey.asText();
                }
            }
        }
        
        // 尝试从 data.nodeId 获取
        JsonNode data = mainsiteContent.path("data");
        if (!data.isMissingNode()) {
            JsonNode nodeId = data.path("nodeId");
            if (!nodeId.isMissingNode()) {
                return nodeId.asText();
            }
        }
        
        throw new RuntimeException("未找到 dentryKey 或 nodeId");
    }
    
    /**
     * 通过 POST 请求获取文档详细数据
     * 
     * @param cookie Cookie 字符串
     * @param dentryKey Dentry Key
     * @return 文档数据 JSON
     */
    private JsonNode fetchDocumentData(String cookie, String dentryKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("a-dentry-key", dentryKey);
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json");
        headers.put("Cookie", cookie);
        headers.put("Origin", baseUrl);
        headers.put("Referer", baseUrl);
        
        Map<String, Object> body = new HashMap<>();
        body.put("fetchBody", true);
        
        return httpClient.post(documentDataUrl, body, headers, JsonNode.class);
    }
    
    /**
     * 从 document_data 中提取文档内容
     * 
     * @param documentData 文档数据 JSON
     * @return 文档内容 JSON（如果存在）
     */
    private JsonNode extractDocumentContent(JsonNode documentData) {
        try {
            JsonNode data = documentData.path("data");
            JsonNode documentContent = data.path("documentContent");
            JsonNode checkpoint = documentContent.path("checkpoint");
            JsonNode contentNode = checkpoint.path("content");
            
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return null;
            }
            
            String contentStr = contentNode.asText();
            return objectMapper.readTree(contentStr);
        } catch (Exception e) {
            log.warn("提取文档内容失败（可能是 OSS 加密）", e);
            return null;
        }
    }
    
    /**
     * 从 mainsite_content 中提取文档标题
     * 
     * @param mainsiteContent mainsite content JSON
     * @return 文档标题
     */
    private String extractDocumentTitle(JsonNode mainsiteContent) {
        try {
            JsonNode dentryInfo = mainsiteContent.path("dentryInfo");
            JsonNode data = dentryInfo.path("data");
            JsonNode name = data.path("name");
            
            if (!name.isMissingNode() && !name.isNull()) {
                return name.asText();
            }
        } catch (Exception e) {
            log.warn("提取文档标题失败", e);
        }
        
        return "钉钉文档";
    }
}
