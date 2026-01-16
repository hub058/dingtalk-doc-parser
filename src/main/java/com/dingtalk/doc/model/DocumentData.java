package com.dingtalk.doc.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档数据模型
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentData {
    
    /**
     * 节点 ID
     */
    private String nodeId;
    
    /**
     * Dentry Key（文档访问密钥）
     */
    private String dentryKey;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * Mainsite 内容（从 HTML 提取的 JSON）
     */
    private JsonNode mainsiteContent;
    
    /**
     * 文档详细数据（从 API 获取）
     */
    private JsonNode documentContent;
    
    /**
     * 解析后的文档内容
     */
    private JsonNode content;
}
