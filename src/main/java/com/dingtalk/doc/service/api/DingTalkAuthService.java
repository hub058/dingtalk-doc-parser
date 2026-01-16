package com.dingtalk.doc.service.api;

import com.dingtalk.doc.config.DingTalkApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 钉钉认证服务
 * 负责获取和管理 AccessToken
 * 
 * 新版API文档：https://open.dingtalk.com/document/development/obtain-the-access-token-of-an-internal-app
 * 
 * @author DingTalk Doc Parser Team
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "dingtalk.api.enabled", havingValue = "true")
public class DingTalkAuthService {
    
    private final DingTalkApiConfig config;
    private final RestTemplate restTemplate;
    
    private String accessToken;
    private long tokenExpireTime;
    
    private final Object lock = new Object();
    
    public DingTalkAuthService(DingTalkApiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }
    
    /**
     * 获取 AccessToken（带缓存和自动刷新）
     * 
     * @return AccessToken
     */
    public String getAccessToken() {
        // 如果 token 未过期，直接返回
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        
        // 加锁刷新（避免并发刷新）
        synchronized (lock) {
            // 双重检查
            if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return accessToken;
            }
            
            return refreshAccessToken();
        }
    }
    
    /**
     * 刷新 AccessToken
     * 
     * 新版API文档：https://open.dingtalk.com/document/development/obtain-the-access-token-of-an-internal-app
     * 
     * 请求方式：POST
     * 请求地址：https://api.dingtalk.com/v1.0/oauth2/accessToken
     * 
     * @return AccessToken
     */
    private String refreshAccessToken() {
        String url = config.getBaseUrl() + "/oauth2/accessToken";
        
        try {
            log.info("开始刷新 AccessToken（新版API）...");
            
            // 构建请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("appKey", config.getAppKey());
            requestBody.put("appSecret", config.getAppSecret());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送POST请求
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            JsonNode body = response.getBody();
            
            if (body != null) {
                // 新版API返回格式
                accessToken = body.path("accessToken").asText();
                int expireIn = body.path("expireIn").asInt(7200); // 默认 7200 秒（2小时）
                
                if (accessToken == null || accessToken.isEmpty()) {
                    throw new RuntimeException("获取 AccessToken 失败: accessToken 为空");
                }
                
                // 提前 5 分钟刷新，避免边界情况
                tokenExpireTime = System.currentTimeMillis() + 
                    (expireIn - config.getTokenRefreshBeforeExpire()) * 1000L;
                
                LocalDateTime expireDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(tokenExpireTime), 
                    ZoneId.systemDefault()
                );
                
                log.info("AccessToken 刷新成功（新版API），有效期至: {}", expireDateTime);
                return accessToken;
            }
            
            throw new RuntimeException("获取 AccessToken 失败: 响应为空");
            
        } catch (Exception e) {
            log.error("刷新 AccessToken 失败", e);
            
            // 如果新版API失败，尝试使用旧版API
            log.warn("新版API失败，尝试使用旧版API...");
            return refreshAccessTokenLegacy();
        }
    }
    
    /**
     * 刷新 AccessToken（旧版API）
     * 
     * 旧版API文档：https://open.dingtalk.com/document/orgapp-server/obtain-orgapp-token
     * 
     * 请求方式：GET
     * 请求地址：https://oapi.dingtalk.com/gettoken?appkey=xxx&appsecret=xxx
     * 
     * @return AccessToken
     */
    private String refreshAccessTokenLegacy() {
        String url = String.format(
            "%s/gettoken?appkey=%s&appsecret=%s",
            config.getBaseUrl(),
            config.getAppKey(),
            config.getAppSecret()
        );
        
        try {
            log.info("开始刷新 AccessToken（旧版API）...");
            
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode body = response.getBody();
            
            if (body != null) {
                int errcode = body.path("errcode").asInt(-1);
                
                if (errcode == 0) {
                    accessToken = body.path("access_token").asText();
                    int expiresIn = body.path("expires_in").asInt(7200); // 默认 7200 秒（2小时）
                    
                    // 提前 5 分钟刷新，避免边界情况
                    tokenExpireTime = System.currentTimeMillis() + 
                        (expiresIn - config.getTokenRefreshBeforeExpire()) * 1000L;
                    
                    LocalDateTime expireDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(tokenExpireTime), 
                        ZoneId.systemDefault()
                    );
                    
                    log.info("AccessToken 刷新成功（旧版API），有效期至: {}", expireDateTime);
                    return accessToken;
                }
                
                String errmsg = body.path("errmsg").asText("未知错误");
                throw new RuntimeException(String.format(
                    "获取 AccessToken 失败: errcode=%d, errmsg=%s", 
                    errcode, errmsg
                ));
            }
            
            throw new RuntimeException("获取 AccessToken 失败: 响应为空");
            
        } catch (Exception e) {
            log.error("刷新 AccessToken 失败（旧版API）", e);
            throw new RuntimeException("刷新 AccessToken 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 强制刷新 Token
     * 
     * @return AccessToken
     */
    public String forceRefresh() {
        synchronized (lock) {
            accessToken = null;
            return refreshAccessToken();
        }
    }
    
    /**
     * 检查 Token 是否有效
     * 
     * @return true 如果有效
     */
    public boolean isTokenValid() {
        return accessToken != null && System.currentTimeMillis() < tokenExpireTime;
    }
    
    /**
     * 获取 Token 剩余有效时间（秒）
     * 
     * @return 剩余秒数
     */
    public long getTokenRemainingTime() {
        if (accessToken == null) {
            return 0;
        }
        
        long remaining = (tokenExpireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
