package com.dingtalk.doc.service.api;

import com.dingtalk.doc.config.DingTalkApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 钉钉 API 客户端
 * 封装 HTTP 请求，自动处理 Token 认证
 *
 * @author DingTalk Doc Parser Team
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "dingtalk.api.enabled", havingValue = "true")
public class DingTalkApiClient {

    private final DingTalkAuthService authService;
    private final DingTalkApiConfig config;
    private final RestTemplate restTemplate;

    public DingTalkApiClient(
            DingTalkAuthService authService,
            DingTalkApiConfig config,
            RestTemplate restTemplate) {
        this.authService = authService;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    /**
     * GET 请求
     *
     * @param path         API 路径
     * @param responseType 响应类型
     * @param <T>          响应类型
     * @return 响应数据
     */
    public <T> T get(String path, Class<T> responseType) {
        String accessToken = authService.getAccessToken();
        String baseUrl = path.startsWith("/v2.0/wiki") ? config.getDocBaseUrl() : config.getBaseUrl();
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.debug("GET 请求: {}", url);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Token 可能过期，强制刷新后重试
                log.warn("Token 可能过期，强制刷新后重试");
                authService.forceRefresh();

                // 重试一次
                headers.set("x-acs-dingtalk-access-token", authService.getAccessToken());
                entity = new HttpEntity<>(headers);

                ResponseEntity<T> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        responseType
                );

                return response.getBody();
            }

            log.error("API 请求失败: {}", e.getMessage());
            throw new RuntimeException("API 请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * GET 请求（旧版 API，使用 access_token 参数）
     *
     * @param path         API 路径（不包含 access_token）
     * @param responseType 响应类型
     * @param <T>          响应类型
     * @return 响应数据
     */
    public <T> T getWithTokenParam(String path, Class<T> responseType) {
        String accessToken = authService.getAccessToken();
        String url = config.getBaseUrl() + path;

        // 添加 access_token 参数
        String separator = url.contains("?") ? "&" : "?";
        url = url + separator + "access_token=" + accessToken;

        try {
            log.debug("GET 请求（Token 参数）: {}", url);

            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            return response.getBody();

        } catch (Exception e) {
            log.error("API 请求失败: {}", e.getMessage());
            throw new RuntimeException("API 请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * POST 请求
     *
     * @param path         API 路径
     * @param requestBody  请求体
     * @param responseType 响应类型
     * @param <T>          响应类型
     * @return 响应数据
     */
    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        String accessToken = authService.getAccessToken();
        String baseUrl = config.getBaseUrl();
        String url = baseUrl + path + "?access_token=" + accessToken;

        HttpHeaders headers = new HttpHeaders();
        //headers.set("x-acs-dingtalk-access-token", accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("POST 请求: {}", url);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Token 可能过期，强制刷新后重试
                log.warn("Token 可能过期，强制刷新后重试");
                authService.forceRefresh();

                // 重试一次
                headers.set("x-acs-dingtalk-access-token", authService.getAccessToken());
                entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<T> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        responseType
                );

                return response.getBody();
            }

            log.error("API 请求失败: {}", e.getMessage());
            throw new RuntimeException("API 请求失败: " + e.getMessage(), e);
        }
    }
}
