package com.dingtalk.doc.model;

import lombok.Data;
import java.util.List;

/**
 * 文档节点
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
public class DocNode {
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 节点名称
     */
    private String name;
    
    /**
     * 节点类型（file: 文件, folder: 文件夹）
     */
    private String type;
    
    /**
     * 文档类型（doc: 文档, sheet: 表格, slide: 演示文稿等）
     */
    private String docType;
    
    /**
     * 父节点ID
     */
    private String parentId;
    
    /**
     * 知识库ID
     */
    private String workspaceId;
    
    /**
     * 文档URL
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
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 修改者ID
     */
    private String modifierId;
    
    /**
     * 是否有子节点
     */
    private Boolean hasChildren;
    
    /**
     * 子节点列表
     */
    private List<DocNode> children;
}
