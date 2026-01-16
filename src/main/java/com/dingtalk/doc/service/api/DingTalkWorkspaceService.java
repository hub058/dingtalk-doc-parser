package com.dingtalk.doc.service.api;

import com.dingtalk.doc.config.DingTalkApiConfig;
import com.dingtalk.doc.model.DocNode;
import com.dingtalk.doc.model.Workspace;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 钉钉知识库服务
 * 
 * API文档：
 * - 获取知识库列表：https://open.dingtalk.com/document/development/get-knowledge-base-list
 * - 获取节点列表：https://open.dingtalk.com/document/development/get-node-list
 * 
 * @author DingTalk Doc Parser Team
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "dingtalk.api.enabled", havingValue = "true")
public class DingTalkWorkspaceService {
    
    private final DingTalkApiClient apiClient;
    private final DingTalkApiConfig config;
    
    public DingTalkWorkspaceService(DingTalkApiClient apiClient, DingTalkApiConfig config) {
        this.apiClient = apiClient;
        this.config = config;
    }
    
    /**
     * 获取知识库列表
     * 
     * API: GET /v1.0/doc/workspaces (尝试文档 API)
     * 备用: GET /v1.0/wiki/workspaces
     * 
     * @return 知识库列表
     */
    public List<Workspace> getWorkspaces() {
        try {
            log.info("开始获取知识库列表...");
            
            // 尝试使用文档 API
            String path = "/v2.0/wiki/workspaces?operatorId=soBMsGl5wpDDDp7UgiS7NgwiEiE";
            JsonNode response = null;
            
            try {
                response = apiClient.get(path, JsonNode.class);
            } catch (Exception e) {
                log.warn("文档 API 失败，尝试知识库 API: {}", e.getMessage());
                // 如果文档 API 失败，尝试知识库 API
                path = "/wiki/workspaces";
                response = apiClient.get(path, JsonNode.class);
            }
            
            List<Workspace> workspaces = new ArrayList<>();
            
            if (response != null) {
                JsonNode workspaceList = response.path("workspaces");
                
                if (workspaceList.isArray()) {
                    for (JsonNode item : workspaceList) {
                        Workspace workspace = parseWorkspace(item);
                        workspaces.add(workspace);
                    }
                }
            }
            
            log.info("成功获取 {} 个知识库", workspaces.size());
            return workspaces;
            
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            throw new RuntimeException("获取知识库列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定知识库的详细信息
     * 
     * API: GET /v1.0/wiki/workspaces/{workspaceId}
     * 
     * @param workspaceId 知识库ID
     * @return 知识库信息
     */
    public Workspace getWorkspace(String workspaceId) {
        try {
            log.info("开始获取知识库详情: {}", workspaceId);
            
            String path = "/wiki/workspaces/" + workspaceId;
            JsonNode response = apiClient.get(path, JsonNode.class);
            
            if (response != null) {
                Workspace workspace = parseWorkspace(response);
                log.info("成功获取知识库详情: {}", workspace.getName());
                return workspace;
            }
            
            throw new RuntimeException("获取知识库详情失败: 响应为空");
            
        } catch (Exception e) {
            log.error("获取知识库详情失败: {}", workspaceId, e);
            throw new RuntimeException("获取知识库详情失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取节点列表
     * 
     * API: GET /v1.0/wiki/workspaces/{workspaceId}/nodes
     * 
     * @param workspaceId 知识库ID
     * @return 节点列表
     */
    public List<DocNode> getNodes(String workspaceId) {
        return getNodes(workspaceId, null);
    }
    
    /**
     * 获取节点列表
     * 
     * API: GET /v1.0/wiki/workspaces/{workspaceId}/nodes
     * 
     * @param workspaceId 知识库ID
     * @param parentNodeId 父节点ID（可选，为null时获取根节点）
     * @return 节点列表
     */
    public List<DocNode> getNodes(String workspaceId, String parentNodeId) {
        try {
            log.info("开始获取节点列表: workspaceId={}, parentNodeId={}", workspaceId, parentNodeId);
            
            String path = "/v2.0/wiki/nodes";
            if (parentNodeId != null && !parentNodeId.isEmpty()) {
                path = String.format("%s?parentNodeId=%s&operatorId=soBMsGl5wpDDDp7UgiS7NgwiEiE",path, parentNodeId);
            }
            
            JsonNode response = apiClient.get(path, JsonNode.class);
            
            List<DocNode> nodes = new ArrayList<>();
            
            if (response != null) {
                JsonNode nodeList = response.path("nodes");
                
                if (nodeList.isArray()) {
                    for (JsonNode item : nodeList) {
                        DocNode node = parseDocNode(item);
                        node.setWorkspaceId(workspaceId);
                        nodes.add(node);
                    }
                }
            }
            
            log.info("成功获取 {} 个节点", nodes.size());
            return nodes;
            
        } catch (Exception e) {
            log.error("获取节点列表失败: workspaceId={}", workspaceId, e);
            throw new RuntimeException("获取节点列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取节点详情
     * 
     * API: GET /v1.0/wiki/workspaces/{workspaceId}/nodes/{nodeId}
     * 
     * @param workspaceId 知识库ID
     * @param nodeId 节点ID
     * @return 节点信息
     */
    public DocNode getNode(String workspaceId, String nodeId) {
        try {
            log.info("开始获取节点详情: workspaceId={}, nodeId={}", workspaceId, nodeId);
            
            String path = "/wiki/workspaces/" + workspaceId + "/nodes/" + nodeId;
            JsonNode response = apiClient.get(path, JsonNode.class);
            
            if (response != null) {
                DocNode node = parseDocNode(response);
                node.setWorkspaceId(workspaceId);
                log.info("成功获取节点详情: {}", node.getName());
                return node;
            }
            
            throw new RuntimeException("获取节点详情失败: 响应为空");
            
        } catch (Exception e) {
            log.error("获取节点详情失败: nodeId={}", nodeId, e);
            throw new RuntimeException("获取节点详情失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取文档内容
     * 
     * API: GET /v1.0/wiki/workspaces/{workspaceId}/nodes/{nodeId}/content
     * 
     * @param workspaceId 知识库ID
     * @param nodeId 节点ID
     * @return 文档内容（JSON格式）
     */
    public JsonNode getDocumentContent(String workspaceId, String nodeId) {
        try {
            log.info("开始获取文档内容: workspaceId={}, nodeId={}", workspaceId, nodeId);
            
            String path = "/i/nodes/"+ nodeId;
            JsonNode response = apiClient.get(path, JsonNode.class);
            
            if (response != null) {
                log.info("成功获取文档内容: nodeId={}", nodeId);
                return response;
            }
            
            throw new RuntimeException("获取文档内容失败: 响应为空");
            
        } catch (Exception e) {
            log.error("获取文档内容失败: nodeId={}", nodeId, e);
            throw new RuntimeException("获取文档内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归获取所有节点（包括子节点）
     * 
     * @param workspaceId 知识库ID
     * @param parentNodeId 父节点ID（可选）
     * @return 所有节点列表
     */
    public List<DocNode> getAllNodesRecursively(String workspaceId, String parentNodeId) {
        List<DocNode> allNodes = new ArrayList<>();
        
        try {
            List<DocNode> nodes = getNodes(workspaceId, parentNodeId);
            
            for (DocNode node : nodes) {
                allNodes.add(node);
                
                // 如果是文件夹且有子节点，递归获取
                if ("folder".equals(node.getType()) && 
                    (node.getHasChildren() == null || node.getHasChildren())) {
                    List<DocNode> children = getAllNodesRecursively(workspaceId, node.getNodeId());
                    node.setChildren(children);
                    allNodes.addAll(children);
                }
            }
            
        } catch (Exception e) {
            log.error("递归获取节点失败: workspaceId={}, parentNodeId={}", workspaceId, parentNodeId, e);
        }
        
        return allNodes;
    }
    
    /**
     * 解析知识库JSON
     * 
     * @param json JSON节点
     * @return 知识库对象
     */
    private Workspace parseWorkspace(JsonNode json) {
        Workspace workspace = new Workspace();
        
        workspace.setWorkspaceId(json.path("workspaceId").asText());
        workspace.setName(json.path("name").asText());
        workspace.setType(json.path("type").asText());
        workspace.setUrl(json.path("url").asText());
        workspace.setCreateTime(json.path("createTime").asLong());
        workspace.setModifiedTime(json.path("modifiedTime").asLong());
        workspace.setOwnerId(json.path("ownerId").asText());
        workspace.setIcon(json.path("icon").path("value").asText());
        workspace.setDescription(json.path("description").asText());
        workspace.setRootNodeId(json.path("rootNodeId").asText());
        
        return workspace;
    }
    
    /**
     * 解析文档节点JSON
     * 
     * @param json JSON节点
     * @return 文档节点对象
     */
    private DocNode parseDocNode(JsonNode json) {
        DocNode node = new DocNode();
        
        node.setNodeId(json.path("nodeId").asText());
        node.setName(json.path("name").asText());
        node.setType(json.path("type").asText());
        node.setDocType(json.path("docType").asText());
        node.setParentId(json.path("parentId").asText());
        node.setUrl(json.path("url").asText());
        node.setCreateTime(json.path("createTime").asLong());
        node.setModifiedTime(json.path("modifiedTime").asLong());
        node.setCreatorId(json.path("creatorId").asText());
        node.setModifierId(json.path("modifierId").asText());
        node.setHasChildren(json.path("hasChildren").asBoolean(false));
        
        return node;
    }
}
