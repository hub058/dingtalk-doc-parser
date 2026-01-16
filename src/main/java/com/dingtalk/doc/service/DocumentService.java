package com.dingtalk.doc.service;

import com.dingtalk.doc.model.DocumentData;
import com.dingtalk.doc.model.DocumentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文档服务
 * 协调文档解析流程，整合各个组件
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final CookieManager cookieManager;
    private final DocumentParser documentParser;
    private final MarkdownGenerator markdownGenerator;
    private final FileManager fileManager;
    
    /**
     * 解析钉钉文档并生成 Markdown 文件
     * 
     * @param documentUrl 钉钉文档 URL 或 Node ID
     * @param providedCookie 用户提供的 Cookie（可选）
     * @return 文档解析结果
     */
    public DocumentResult parseDocument(String documentUrl, String providedCookie) {
        log.info("开始解析文档: {}", documentUrl);
        
        try {
            // 步骤1: 获取有效的 Cookie
            log.info("步骤 1/5: 获取有效 Cookie");
            String cookie = cookieManager.getValidCookie(providedCookie);
            
            // 步骤2: 解析文档数据
            log.info("步骤 2/5: 解析文档数据");
            DocumentData documentData = documentParser.getCompleteDocumentData(documentUrl, cookie);
            
            // 步骤3: 准备输出目录
            log.info("步骤 3/5: 准备输出目录");
            String outputDir = fileManager.prepareOutputDirectory(documentData.getTitle());
            
            // 步骤4: 生成 Markdown（包含图片下载）
            log.info("步骤 4/5: 生成 Markdown 并下载图片");
            String markdown = markdownGenerator.generateMarkdown(
                documentData.getContent(), 
                documentData.getTitle(),
                cookie,
                outputDir
            );
            
            if (markdown == null || markdown.trim().isEmpty()) {
                throw new RuntimeException("生成的 Markdown 内容为空");
            }
            
            // 步骤5: 保存文件
            log.info("步骤 5/5: 保存 Markdown 文件");
            String filePath = fileManager.saveMarkdownFile(documentData.getTitle(), markdown);
            
            log.info("文档解析完成: {}", filePath);
            
            return DocumentResult.builder()
                    .nodeId(documentData.getNodeId())
                    .title(documentData.getTitle())
                    .filePath(filePath)
                    .markdown(markdown)
                    .build();
        } catch (Exception e) {
            log.error("文档解析失败", e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
