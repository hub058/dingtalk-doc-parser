package com.dingtalk.doc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import javax.net.ssl.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用配置类
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "dingtalk")
public class AppConfig {
    
    /**
     * API 配置
     */
    private Api api = new Api();
    
    /**
     * Cookie 配置
     */
    private String cookie;
    
    @Value("${http.client.disable-ssl-validation:true}")
    private boolean disableSslValidation;
    
    /**
     * 配置 RestTemplate Bean，支持 UTF-8 编码
     * 解决中文乱码问题
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(api.getTimeout());
        factory.setReadTimeout(api.getTimeout());
        // 重要：设置为 false，让我们手动处理响应体
        factory.setBufferRequestBody(false);
        
        // 禁用 SSL 证书验证（仅用于开发环境）
        if (disableSslValidation) {
            disableSslVerification();
        }
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 配置 UTF-8 字符集，解决中文乱码
        restTemplate.getMessageConverters().stream()
            .filter(converter -> converter instanceof StringHttpMessageConverter)
            .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        return restTemplate;
    }
    
    /**
     * 禁用 SSL 证书验证
     * 注意：仅用于开发环境，生产环境应使用有效证书
     */
    private void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable SSL verification", e);
        }
    }
    
    @Data
    public static class Api {
        /**
         * 钉钉 API 基础 URL
         */
        @NotBlank(message = "钉钉 API 基础 URL 不能为空")
        private String baseUrl = "https://alidocs.dingtalk.com";
        
        /**
         * 文档数据 API URL
         */
        private String documentDataUrl;
        
        /**
         * 请求超时时间（毫秒）
         */
        @Positive(message = "超时时间必须大于 0")
        private int timeout = 30000;
    }
}
