package com.dingtalk.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 钉钉开放平台 API 配置
 * 
 * @author DingTalk Doc Parser Team
 */
@Configuration
@ConfigurationProperties(prefix = "dingtalk.api")
@Data
public class DingTalkApiConfig {
    
    /**
     * 是否启用钉钉 API
     */
    private boolean enabled = false;
    
    /**
     * 应用 Key（AppKey）
     */
    private String appKey;
    
    /**
     * 应用密钥（AppSecret）
     */
    private String appSecret;
    
    /**
     * 企业 ID（CorpId）
     */
    private String corpId;
    
    /**
     * API 基础 URL
     */
    private String baseUrl = "https://oapi.dingtalk.com";
    /**
     * API 基础 URL
     */
    private String docBaseUrl = "https://api.dingtalk.com";
    /**
     * API 基础 URL
     */
    private String aliDocBaseUrl = "https://alidocs.dingtalk.com";

    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;
    
    /**
     * Token 刷新提前时间（秒）
     */
    private int tokenRefreshBeforeExpire = 300;
}
