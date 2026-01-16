package com.dingtalk.doc.service.api;

import com.dingtalk.doc.config.DingTalkApiConfig;
import com.dingtalk.doc.model.DingTalkUser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 钉钉用户服务
 * 
 * API文档：
 * - 获取用户详情：https://open.dingtalk.com/document/development/query-user-details
 * 
 * @author DingTalk Doc Parser Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dingtalk.api.enabled", havingValue = "true")
public class DingTalkUserService {
    
    private final DingTalkApiClient apiClient;
    private final DingTalkApiConfig config;
    
    /**
     * 根据 userId 获取用户详情
     * 
     * API: GET /v1.0/contact/users/{userId}
     * 
     * @param userId 用户ID
     * @return 用户详情
     */
    public DingTalkUser getUserByUserId(String userId) {
        try {
            log.info("开始获取用户详情: userId={}", userId);
            
            String path = "/topapi/v2/user/get";
            // 构建请求体
            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("userid", userId);
            JsonNode response = apiClient.post(path, requestBody, JsonNode.class);
            
            if (response != null) {
                DingTalkUser user = parseUser(response);
                log.info("成功获取用户详情: {}", user.getName());
                return user;
            }
            
            throw new RuntimeException("获取用户详情失败: 响应为空");
            
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            throw new RuntimeException("获取用户详情失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据 userId 获取用户的 unionid
     * 
     * @param userId 用户ID
     * @return unionid
     */
    public String getUserUnionId(String userId) {
        try {
            log.info("开始获取用户 unionid: userId={}", userId);
            
            DingTalkUser user = getUserByUserId(userId);
            
            if (user != null && user.getUnionid() != null) {
                log.info("成功获取 unionid: {}", user.getUnionid());
                return user.getUnionid();
            }
            
            throw new RuntimeException("获取 unionid 失败: 用户信息中没有 unionid");
            
        } catch (Exception e) {
            log.error("获取 unionid 失败: userId={}", userId, e);
            throw new RuntimeException("获取 unionid 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据手机号获取用户详情
     * 
     * API: POST /v1.0/contact/users/getByMobile
     * 
     * @param mobile 手机号
     * @return 用户详情
     */
    public String getUserByMobile(String mobile) {
        try {
            log.info("开始根据手机号获取用户详情: mobile={}", mobile);
            
            String path = "/topapi/v2/user/getbymobile";
            
            // 构建请求体
            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("mobile", mobile);
            
            JsonNode response = apiClient.post(path, requestBody, JsonNode.class);
            
            if (response != null) {
                return  response.get("result").get("userid").asText();
            }
            
            throw new RuntimeException("获取用户详情失败: 响应为空");
            
        } catch (Exception e) {
            log.error("根据手机号获取用户详情失败: mobile={}", mobile, e);
            throw new RuntimeException("根据手机号获取用户详情失败: " + e.getMessage(), e);
        }
    }

    
    /**
     * 解析用户JSON
     * 
     * @param json JSON节点
     * @return 用户对象
     */
    private DingTalkUser parseUser(JsonNode json) {
        DingTalkUser user = new DingTalkUser();
        json = json.path("result");
        user.setUserid(json.path("userid").asText());
        user.setUnionid(json.path("unionid").asText());
        user.setName(json.path("name").asText());
        user.setMobile(json.path("mobile").asText());
        user.setEmail(json.path("email").asText());
        user.setAvatar(json.path("avatar").asText());
        user.setTitle(json.path("title").asText());
        user.setJobNumber(json.path("jobNumber").asText());
        user.setActive(json.path("active").asBoolean());
        user.setAdmin(json.path("admin").asBoolean());
        user.setBoss(json.path("boss").asBoolean());
        user.setHiredDate(json.path("hiredDate").asLong());
        user.setRemark(json.path("remark").asText());
        
        // 解析部门ID列表
        JsonNode deptIdListNode = json.path("deptIdList");
        if (deptIdListNode.isArray()) {
            Long[] deptIds = new Long[deptIdListNode.size()];
            for (int i = 0; i < deptIdListNode.size(); i++) {
                deptIds[i] = deptIdListNode.get(i).asLong();
            }
            user.setDeptIdList(deptIds);
        }
        
        return user;
    }
}
