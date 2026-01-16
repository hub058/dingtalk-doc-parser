package com.dingtalk.doc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件管理器
 * 负责文件系统操作，包括创建目录、保存文件
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Component
public class FileManager {
    
    @Value("${file.output.base-dir}")
    private String baseDir;
    
    /**
     * 获取绝对路径的基础目录
     * 如果是相对路径，则相对于项目根目录
     * 
     * @return 绝对路径
     */
    private Path getAbsoluteBaseDir() {
        Path path = Paths.get(baseDir);
        if (path.isAbsolute()) {
            return path;
        }
        // 相对路径：相对于项目根目录
        return Paths.get(System.getProperty("user.dir"), baseDir).toAbsolutePath();
    }
    
    /**
     * 保存 Markdown 文件
     * 
     * @param title 文档标题
     * @param content Markdown 内容
     * @return 文件完整路径
     */
    public String saveMarkdownFile(String title, String content) {
        try {
            // 清理文件名
            String cleanTitle = sanitizeFilename(title);
            
            // 创建输出目录（使用绝对路径）
            Path outputDir = getAbsoluteBaseDir().resolve(cleanTitle);
            ensureDirectory(outputDir);
            
            // 创建文件路径
            Path filePath = outputDir.resolve(cleanTitle + ".md");
            
            // 写入文件
            Files.writeString(filePath, content);
            
            String absolutePath = filePath.toAbsolutePath().toString();
            log.info("Markdown 文件已保存: {}", absolutePath);
            
            return absolutePath;
        } catch (IOException e) {
            log.error("保存 Markdown 文件失败", e);
            throw new RuntimeException("保存 Markdown 文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理文件名中的非法字符
     * 
     * @param filename 原始文件名
     * @return 清理后的文件名
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "未命名文档";
        }
        
        // 定义非法字符
        char[] illegalChars = {'/', '\\', ':', '*', '?', '"', '<', '>', '|'};
        
        String clean = filename;
        
        // 替换非法字符为下划线
        for (char illegalChar : illegalChars) {
            clean = clean.replace(illegalChar, '_');
        }
        
        // 移除 .adoc 后缀（如果存在）
        if (clean.toLowerCase().endsWith(".adoc")) {
            clean = clean.substring(0, clean.length() - 5);
        }
        
        // 去除首尾空格
        clean = clean.trim();
        
        // 如果清理后为空，使用默认名称
        if (clean.isEmpty()) {
            clean = "未命名文档";
        }
        
        // 限制文件名长度（最多 200 个字符）
        if (clean.length() > 200) {
            clean = clean.substring(0, 200);
        }
        
        return clean;
    }
    
    /**
     * 确保目录存在，不存在则创建
     * 
     * @param directory 目录路径
     */
    private void ensureDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.debug("创建目录: {}", directory);
        }
    }
    
    /**
     * 获取基础输出目录
     * 
     * @return 基础目录路径
     */
    public String getBaseDir() {
        return baseDir;
    }
    
    /**
     * 准备输出目录（用于保存 Markdown 和图片）
     * 
     * @param title 文档标题
     * @return 输出目录的绝对路径
     */
    public String prepareOutputDirectory(String title) {
        try {
            // 清理文件名
            String cleanTitle = sanitizeFilename(title);
            
            // 创建输出目录（使用绝对路径）
            Path outputDir = getAbsoluteBaseDir().resolve(cleanTitle);
            ensureDirectory(outputDir);
            
            String absolutePath = outputDir.toAbsolutePath().toString();
            log.info("输出目录已准备: {}", absolutePath);
            
            return absolutePath;
        } catch (IOException e) {
            log.error("准备输出目录失败", e);
            throw new RuntimeException("准备输出目录失败: " + e.getMessage(), e);
        }
    }
}
