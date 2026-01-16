package com.dingtalk.doc.service;

import com.dingtalk.doc.model.Cookie;
import com.dingtalk.doc.model.CookieData;
import com.dingtalk.doc.repository.CookieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cookie 管理器
 * 负责 Cookie 的获取、验证、保存和加载
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CookieManager {
    
    private final CookieRepository cookieRepository;
    private final DingTalkHttpClient httpClient;
    
    @Value("${dingtalk.api.base-url}")
    private String baseUrl;
    
    /**
     * 获取有效的 Cookie
     * 优先级：手动提供 > 环境变量 > 本地文件
     * 
     * @param providedCookie 手动提供的 Cookie（可选）
     * @return 有效的 Cookie 字符串
     */
    public String getValidCookie(String providedCookie) {
        log.info("开始获取有效 Cookie");
        
        // 1. 如果手动提供了 Cookie，优先使用（即使验证失败也使用）
        if (providedCookie != null && !providedCookie.trim().isEmpty()) {
            log.info("使用手动提供的 Cookie");
            boolean isValid = validateCookie(providedCookie);
            if (isValid) {
                log.info("手动提供的 Cookie 验证通过");
            } else {
                log.warn("手动提供的 Cookie 验证失败，但仍然使用它（可能是验证逻辑误判）");
            }
            return providedCookie;
        }
        
        // 2. 尝试从环境变量获取
        String envCookie = System.getenv("DINGTALK_COOKIE");
        if (envCookie != null && !envCookie.trim().isEmpty()) {
            log.info("尝试使用环境变量 Cookie");
            if (validateCookie(envCookie)) {
                log.info("环境变量 Cookie 有效");
                return envCookie;
            }
            log.warn("环境变量 Cookie 无效");
        }
        
        // 3. 尝试从本地文件加载
        CookieData savedCookieData = loadCookie();
        if (savedCookieData != null) {
            String savedCookie = savedCookieData.getCookieString();
            log.info("尝试使用本地文件 Cookie");
            if (validateCookie(savedCookie)) {
                log.info("本地文件 Cookie 有效");
                return savedCookie;
            }
            log.warn("本地文件 Cookie 无效");
        }
        
        // 4. 所有方式都失败，抛出异常
        throw new RuntimeException(
            "缺少有效的钉钉 Cookie。请使用以下方式之一：\n\n" +
            "方式 1 - 环境变量：\n" +
            "  export DINGTALK_COOKIE=\"your_cookie_here\"\n\n" +
            "方式 2 - 配置文件：\n" +
            "  在 application.properties 中添加：dingtalk.cookie=your_cookie_here\n\n" +
            "方式 3 - API 请求时提供：\n" +
            "  在请求体中包含 cookie 字段\n\n" +
            "获取 Cookie 方法：\n" +
            "  1. 浏览器打开 https://alidocs.dingtalk.com\n" +
            "  2. 登录钉钉账号\n" +
            "  3. 按 F12 → Network → 复制 Cookie"
        );
    }
    
    /**
     * 验证 Cookie 是否有效
     * 通过发送 HTTP 请求到钉钉 API 检查响应状态
     * 
     * @param cookie Cookie 字符串
     * @return true 如果 Cookie 有效
     */
    public boolean validateCookie(String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) {
            return false;
        }
        
        try {
            log.debug("验证 Cookie 有效性");
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookie);
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            String response = httpClient.get(baseUrl, headers);
            
            log.debug("Cookie 验证响应长度: {} 字符", response.length());
            
            // 检查响应是否包含明确的登录要求标识
            // 注意：只检查最明确的登录标识，避免误判
            boolean needsLogin = response.contains("needLogin: true") || 
                                 response.contains("needLogin:true");
            
            if (needsLogin) {
                log.warn("Cookie 验证失败：响应包含 needLogin 标识");
                return false;
            }
            
            // 检查是否包含正常页面的标识
            boolean hasValidContent = response.contains("<!DOCTYPE") || 
                                     response.contains("<html");
            
            if (!hasValidContent) {
                log.warn("Cookie 验证失败：响应内容异常");
                return false;
            }
            
            log.info("Cookie 验证成功");
            return true;
            
        } catch (Exception e) {
            log.error("Cookie 验证过程中发生错误", e);
            // 网络错误时保守认为 Cookie 可能有效
            return true;
        }
    }
    
    /**
     * 保存 Cookie 到本地文件
     * 
     * @param cookieString Cookie 字符串
     */
    public void saveCookie(String cookieString) {
        log.info("保存 Cookie 到本地文件");
        
        CookieData cookieData = CookieData.builder()
                .cookieString(cookieString)
                .timestamp(LocalDateTime.now())
                .mode("manual")
                .build();
        
        cookieRepository.save(cookieData);
        log.info("Cookie 保存成功");
    }
    
    /**
     * 从本地文件加载 Cookie
     * 
     * @return Cookie 数据（如果存在）
     */
    public CookieData loadCookie() {
        log.debug("从本地文件加载 Cookie");
        return cookieRepository.load().orElse(null);
    }
    
    /**
     * 删除本地 Cookie 文件
     */
    public void deleteCookie() {
        log.info("删除本地 Cookie 文件");
        cookieRepository.delete();
    }
}
