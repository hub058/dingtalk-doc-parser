package com.dingtalk.doc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 钉钉 HTTP 客户端
 * 封装 HTTP 请求，配置请求头和超时
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkHttpClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${dingtalk.api.base-url}")
    private String baseUrl;
    
    @Value("${http.client.user-agent}")
    private String userAgent;
    
    /**
     * 发送 GET 请求
     * 
     * @param url 请求 URL
     * @param headers 请求头
     * @return 响应内容
     */
    public String get(String url, Map<String, String> headers) {
        log.debug("发送 GET 请求: {}", url);
        
        HttpHeaders httpHeaders = createHeaders(headers);
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
            );
            
            log.debug("GET 请求成功，状态码: {}", response.getStatusCode());
            
            // 从响应中获取字节数组
            byte[] responseBody = response.getBody();
            if (responseBody == null || responseBody.length == 0) {
                log.warn("响应体为空");
                return "";
            }
            
            log.debug("响应体大小: {} 字节", responseBody.length);
            
            // 检查是否是 gzip 压缩的响应
            String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
            log.debug("Content-Encoding: {}", contentEncoding);
            
            // 检查前几个字节（用于调试）
            if (responseBody.length >= 10) {
                StringBuilder hexStr = new StringBuilder();
                for (int i = 0; i < Math.min(10, responseBody.length); i++) {
                    hexStr.append(String.format("%02x ", responseBody[i]));
                }
                log.debug("响应体前 10 字节: {}", hexStr.toString());
            }
            
            // 只有明确检测到 gzip 时才解压缩
            boolean isGzip = isGzipCompressed(responseBody);
            log.debug("是否为 gzip 压缩: {}", isGzip);
            
            if (isGzip) {
                log.info("检测到 gzip 压缩，正在解压缩...");
                try {
                    responseBody = decompressGzip(responseBody);
                    log.info("解压缩成功，解压后大小: {} 字节", responseBody.length);
                } catch (Exception e) {
                    log.error("解压缩失败，尝试直接使用原始数据", e);
                    // 如果解压缩失败，继续使用原始数据
                }
            }
            
            // 尝试从 Content-Type 中获取字符集
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null && contentType.getCharset() != null) {
                log.debug("使用响应头指定的字符集: {}", contentType.getCharset());
                return new String(responseBody, contentType.getCharset());
            }
            
            // 默认使用 UTF-8
            log.debug("使用默认 UTF-8 字符集");
            String result = new String(responseBody, StandardCharsets.UTF_8);
            log.debug("解码后字符串长度: {} 字符", result.length());
            
            return result;
            
        } catch (Exception e) {
            log.error("GET 请求失败: {}", url, e);
            throw new RuntimeException("HTTP GET 请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查字节数组是否是 gzip 压缩的
     * gzip 文件的魔数是 0x1f 0x8b
     */
    private boolean isGzipCompressed(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        // GZIP 魔数：0x1f 0x8b (31, 139)
        int byte1 = data[0] & 0xFF;
        int byte2 = data[1] & 0xFF;
        boolean isGzip = (byte1 == 0x1f && byte2 == 0x8b);
        
        if (isGzip) {
            log.debug("检测到 GZIP 魔数: 0x{} 0x{}", 
                String.format("%02x", byte1), 
                String.format("%02x", byte2));
        }
        
        return isGzip;
    }
    
    /**
     * 解压缩 gzip 数据
     */
    private byte[] decompressGzip(byte[] compressed) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("解压缩 gzip 失败", e);
            throw new RuntimeException("解压缩 gzip 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送 POST 请求
     * 
     * @param url 请求 URL
     * @param body 请求体
     * @param headers 请求头
     * @param responseType 响应类型
     * @return 响应对象
     */
    public <T> T post(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        log.debug("发送 POST 请求: {}", url);
        
        HttpHeaders httpHeaders = createHeaders(headers);
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
        
        try {
            // 先获取字节数组响应
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                byte[].class
            );
            
            log.debug("POST 请求成功，状态码: {}", response.getStatusCode());
            
            // 从响应中获取字节数组
            byte[] responseBody = response.getBody();
            if (responseBody == null || responseBody.length == 0) {
                log.warn("POST 响应体为空");
                return null;
            }
            
            log.debug("POST 响应体大小: {} 字节", responseBody.length);
            
            // 检查是否是 gzip 压缩的响应
            String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
            log.debug("POST Content-Encoding: {}", contentEncoding);
            
            // 如果是 gzip 压缩，先解压缩
            boolean isGzip = isGzipCompressed(responseBody);
            log.debug("POST 是否为 gzip 压缩: {}", isGzip);
            
            if (isGzip) {
                log.info("POST 响应检测到 gzip 压缩，正在解压缩...");
                try {
                    responseBody = decompressGzip(responseBody);
                    log.info("POST 解压缩成功，解压后大小: {} 字节", responseBody.length);
                } catch (Exception e) {
                    log.error("POST 解压缩失败，尝试直接使用原始数据", e);
                }
            }
            
            // 将字节数组转换为字符串，然后解析为目标类型
            String responseStr = new String(responseBody, StandardCharsets.UTF_8);
            log.debug("POST 响应字符串长度: {} 字符", responseStr.length());
            
            // 如果目标类型是 String，直接返回
            if (responseType == String.class) {
                return responseType.cast(responseStr);
            }
            
            // 如果目标类型是 JsonNode，使用 ObjectMapper 解析
            if (responseType == com.fasterxml.jackson.databind.JsonNode.class) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return responseType.cast(mapper.readTree(responseStr));
            }
            
            // 其他类型，使用 ObjectMapper 解析
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(responseStr, responseType);
            
        } catch (Exception e) {
            log.error("POST 请求失败: {}", url, e);
            throw new RuntimeException("HTTP POST 请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建 HTTP 请求头
     */
    private HttpHeaders createHeaders(Map<String, String> customHeaders) {
        HttpHeaders headers = new HttpHeaders();
        
        // 添加默认请求头（与 Node.js 版本保持一致）
        headers.set("User-Agent", userAgent);
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        
        // 添加自定义请求头
        if (customHeaders != null) {
            customHeaders.forEach(headers::set);
        }
        
        return headers;
    }

    /**
     * 下载图片到本地
     * 
     * @param imageUrl 图片 URL
     * @param cookie Cookie 字符串
     * @param outputPath 输出路径
     */
    public void downloadImage(String imageUrl, String cookie, String outputPath) {
        log.debug("下载图片: {}", imageUrl);
        String fullUrl = imageUrl;
        if (!imageUrl.startsWith("http")) {
            fullUrl = baseUrl + imageUrl;
        }
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        headers.put("Referer", baseUrl + "/");
        headers.put("Cookie", cookie);
        
        HttpHeaders httpHeaders = createHeaders(headers);
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                byte[].class
            );
            
            byte[] imageData = response.getBody();
            if (imageData == null || imageData.length == 0) {
                log.warn("图片数据为空: {}", imageUrl);
                return;
            }
            
            log.debug("图片下载成功，大小: {} 字节", imageData.length);
            
            // 保存到文件
            java.nio.file.Path path = java.nio.file.Paths.get(outputPath);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, imageData);
            
            log.info("图片已保存: {}", outputPath);
        } catch (Exception e) {
            log.error("下载图片失败: {}", imageUrl, e);
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        }
    }
}
