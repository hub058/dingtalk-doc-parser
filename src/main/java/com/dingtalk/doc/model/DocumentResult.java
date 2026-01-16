package com.dingtalk.doc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档解析结果模型
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResult {
    
    /**
     * 节点 ID
     */
    private String nodeId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 生成的 Markdown 文件路径
     */
    private String filePath;
    
    /**
     * Markdown 内容
     */
    private String markdown;
}
