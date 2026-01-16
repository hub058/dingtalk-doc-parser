package com.dingtalk.doc.repository;

import com.dingtalk.doc.model.CookieData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Cookie 数据持久化仓库
 * 负责 Cookie 的保存、加载和删除
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Repository
public class CookieRepository {
    
    private final ObjectMapper objectMapper;
    private final String cookieFilePath;
    
    public CookieRepository(@Value("${cookie.file.path:dingtalk_cookies.json}") String cookieFilePath) {
        this.cookieFilePath = cookieFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 保存 Cookie 数据到本地文件
     * 
     * @param cookieData Cookie 数据
     */
    public void save(CookieData cookieData) {
        try {
            File file = new File(cookieFilePath);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, cookieData);
            log.info("Cookie 已保存到: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存 Cookie 失败", e);
            throw new RuntimeException("保存 Cookie 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从本地文件加载 Cookie 数据
     * 
     * @return Cookie 数据（如果存在）
     */
    public Optional<CookieData> load() {
        try {
            File file = new File(cookieFilePath);
            if (!file.exists()) {
                log.debug("Cookie 文件不存在: {}", cookieFilePath);
                return Optional.empty();
            }
            
            CookieData cookieData = objectMapper.readValue(file, CookieData.class);
            log.info("Cookie 已从文件加载: {}", file.getAbsolutePath());
            return Optional.of(cookieData);
        } catch (IOException e) {
            log.error("加载 Cookie 失败", e);
            return Optional.empty();
        }
    }
    
    /**
     * 删除 Cookie 文件
     */
    public void delete() {
        try {
            Path path = Paths.get(cookieFilePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Cookie 文件已删除: {}", cookieFilePath);
            } else {
                log.warn("Cookie 文件不存在，无需删除: {}", cookieFilePath);
            }
        } catch (IOException e) {
            log.error("删除 Cookie 文件失败", e);
            throw new RuntimeException("删除 Cookie 文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查 Cookie 文件是否存在
     * 
     * @return true 如果文件存在
     */
    public boolean exists() {
        return new File(cookieFilePath).exists();
    }
}
