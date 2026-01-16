package com.dingtalk.doc.model;

import lombok.Data;

/**
 * 知识库（工作空间）
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
public class Workspace {
    
    /**
     * 知识库ID
     */
    private String workspaceId;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库类型
     */
    private String type;
    
    /**
     * 知识库URL
     */
    private String url;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 修改时间
     */
    private Long modifiedTime;
    
    /**
     * 所有者ID
     */
    private String ownerId;
    
    /**
     * 图标URL
     */
    private String icon;
    
    /**
     * 描述
     */
    private String description;

    /**
     * 根节点ID
     */
    private String rootNodeId;
}
