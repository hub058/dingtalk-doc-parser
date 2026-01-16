package com.dingtalk.doc.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Markdown 生成器
 * 将钉钉文档内容转换为 Markdown 格式
 * 
 * @author DingTalk Doc Parser Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownGenerator {
    
    private final DingTalkHttpClient httpClient;
    
    // 当前文档的 Cookie（用于下载图片）
    private String currentCookie;
    
    // 当前文档的输出目录
    private String currentOutputDir;
    
    // 当前文档的 parts（用于解析文档引用）
    private JsonNode currentParts;
    
    // 图片计数器
    private int imageCounter = 0;
    
    /**
     * 代码语言映射表
     */
    private static final Map<String, String> CODE_LANGUAGE_MAP = new HashMap<>();
    
    static {
        CODE_LANGUAGE_MAP.put("text/x-java", "java");
        CODE_LANGUAGE_MAP.put("text/x-python", "python");
        CODE_LANGUAGE_MAP.put("text/x-javascript", "javascript");
        CODE_LANGUAGE_MAP.put("text/x-go", "go");
        CODE_LANGUAGE_MAP.put("text/x-c++", "cpp");
        CODE_LANGUAGE_MAP.put("text/x-sql", "sql");
        CODE_LANGUAGE_MAP.put("text/x-sh", "bash");
        CODE_LANGUAGE_MAP.put("text/plain", "text");
        CODE_LANGUAGE_MAP.put("application/json", "json");
        CODE_LANGUAGE_MAP.put("text/html", "html");
        CODE_LANGUAGE_MAP.put("text/css", "css");
    }
    
    /**
     * 从文档内容生成 Markdown
     * 
     * @param content 文档内容 JSON
     * @param title 文档标题
     * @param cookie Cookie（用于下载图片）
     * @param outputDir 输出目录
     * @return Markdown 内容
     */
    public String generateMarkdown(JsonNode content, String title, String cookie, String outputDir) {
        if (content == null) {
            log.warn("文档内容为空，无法生成 Markdown");
            return "";
        }
        
        try {
            StringBuilder markdown = new StringBuilder();
            
            // 添加文档标题作为一级标题
            markdown.append("# ").append(title).append("\n\n");
            
            // 获取 main key
            JsonNode mainNode = content.path("main");
            if (mainNode.isMissingNode()) {
                log.warn("未找到 main 节点");
                return markdown.toString();
            }
            
            String mainKey = mainNode.asText();
            
            // 获取 parts
            JsonNode parts = content.path("parts");
            if (parts.isMissingNode()) {
                log.warn("未找到 parts 节点");
                return markdown.toString();
            }
            
            // 设置当前上下文
            this.currentCookie = cookie;
            this.currentOutputDir = outputDir;
            this.currentParts = parts;
            this.imageCounter = 0;
            
            // 获取 main part
            JsonNode mainPart = parts.path(mainKey);
            if (mainPart.isMissingNode()) {
                log.warn("未找到 main part");
                return markdown.toString();
            }
            
            // 获取 body
            JsonNode data = mainPart.path("data");
            JsonNode body = data.path("body");
            
            if (body.isMissingNode() || !body.isArray()) {
                log.warn("未找到 body 或 body 不是数组");
                return markdown.toString();
            }
            
            // 解析 body 中的元素（跳过前两个元素，它们是元数据）
            for (int i = 2; i < body.size(); i++) {
                JsonNode item = body.get(i);
                if (!item.isArray() || item.size() == 0) {
                    continue;
                }
                
                String tag = item.get(0).asText();
                String parsedMarkdown = "";
                
                switch (tag) {
                    case "table":
                        parsedMarkdown = parseTable(item);
                        break;
                    case "code":
                        parsedMarkdown = parseCodeBlock(item);
                        break;
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        parsedMarkdown = parseHeading(item);
                        break;
                    case "ul":
                    case "ol":
                        parsedMarkdown = parseList(item);
                        break;
                    case "blockquote":
                        parsedMarkdown = parseBlockquote(item);
                        break;
                    case "tag":
                        parsedMarkdown = parseTag(item);
                        break;
                    case "p":
                    case "img":
                        parsedMarkdown = parseParagraph(item);
                        break;
                    default:
                        // 尝试作为段落解析
                        parsedMarkdown = parseParagraph(item);
                }
                
                if (!parsedMarkdown.isEmpty()) {
                    markdown.append(parsedMarkdown).append("\n\n");
                }
            }
            
            return markdown.toString().trim();
        } catch (Exception e) {
            log.error("生成 Markdown 失败", e);
            throw new RuntimeException("生成 Markdown 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 转义 Markdown 特殊字符
     * 
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        
        // 转义 Markdown 特殊字符
        return text.replace("\\", "\\\\")
                   .replace("`", "\\`")
                   .replace("*", "\\*")
                   .replace("_", "\\_")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("#", "\\#")
                   .replace("+", "\\+")
                   .replace("-", "\\-")
                   .replace(".", "\\.")
                   .replace("!", "\\!");
    }
    
    /**
     * 解析文本样式（粗体、斜体）
     * 
     * @param spanElement span 元素
     * @return Markdown 文本
     */
    private String parseTextStyle(JsonNode spanElement) {
        return parseTextStyle(spanElement, true);
    }
    
    /**
     * 解析文本样式（粗体、斜体）
     * 
     * @param spanElement span 元素
     * @param applyBold 是否应用粗体样式
     * @return Markdown 文本
     */
    private String parseTextStyle(JsonNode spanElement, boolean applyBold) {
        if (!spanElement.isArray() || spanElement.size() < 2) {
            return "";
        }
        
        String tag = spanElement.get(0).asText();
        if (!"span".equals(tag)) {
            return "";
        }
        
        JsonNode attrs = spanElement.get(1);
        StringBuilder result = new StringBuilder();
        
        // 检查是否有粗体样式
        boolean isBold = applyBold && attrs.has("bold") && attrs.get("bold").asBoolean();
        
        // 遍历子元素
        for (int i = 2; i < spanElement.size(); i++) {
            JsonNode child = spanElement.get(i);
            
            if (child.isTextual()) {
                String text = child.asText();
                if (isBold) {
                    result.append("**").append(text).append("**");
                } else {
                    result.append(text);
                }
            } else if (child.isArray()) {
                result.append(parseTextStyle(child, applyBold));
            }
        }
        
        return result.toString();
    }
    
    /**
     * 解析段落元素
     * 
     * @param paraElement 段落元素
     * @return Markdown 段落
     */
    private String parseParagraph(JsonNode paraElement) {
        if (!paraElement.isArray() || paraElement.size() < 2) {
            return "";
        }
        
        String tag = paraElement.get(0).asText();
        
        if ("p".equals(tag)) {
            JsonNode attrs = paraElement.get(1);
            
            // 检查是否是列表项（带有 list 属性）
            if (attrs.has("list")) {
                return parseListItem(paraElement);
            }
            
            StringBuilder content = new StringBuilder();
            
            for (int i = 2; i < paraElement.size(); i++) {
                JsonNode child = paraElement.get(i);
                
                if (child.isArray() && child.size() > 0) {
                    String childTag = child.get(0).asText();
                    if ("img".equals(childTag)) {
                        content.append(parseImage(child));
                    } else if ("a".equals(childTag)) {
                        content.append(parseLink(child));
                    } else if ("tag".equals(childTag)) {
                        content.append(parseTag(child));
                    } else {
                        content.append(parseTextStyle(child));
                    }
                } else if (child.isTextual()) {
                    content.append(child.asText());
                }
            }
            
            String paragraphText = content.toString().trim();
            return paragraphText.isEmpty() ? "" : paragraphText;
        }
        
        if ("img".equals(tag)) {
            return parseImage(paraElement);
        }
        
        return "";
    }
    
    /**
     * 解析列表项（带有 list 属性的段落）
     * 
     * @param listItemElement 列表项元素
     * @return Markdown 列表项
     */
    private String parseListItem(JsonNode listItemElement) {
        if (!listItemElement.isArray() || listItemElement.size() < 2) {
            return "";
        }
        
        JsonNode attrs = listItemElement.get(1);
        JsonNode listInfo = attrs.path("list");
        
        // 获取列表级别（用于缩进）
        int level = listInfo.has("level") ? listInfo.get("level").asInt() : 0;
        String indent = "  ".repeat(level); // 每级缩进 2 个空格
        
        // 判断是否是有序列表
        boolean isOrdered = listInfo.has("isOrdered") && listInfo.get("isOrdered").asBoolean();
        
        // 构建列表项内容
        StringBuilder content = new StringBuilder();
        for (int i = 2; i < listItemElement.size(); i++) {
            JsonNode child = listItemElement.get(i);
            
            if (child.isArray() && child.size() > 0) {
                String childTag = child.get(0).asText();
                if ("span".equals(childTag)) {
                    content.append(parseTextStyle(child));
                } else if ("a".equals(childTag)) {
                    content.append(parseLink(child));
                } else if ("tag".equals(childTag)) {
                    content.append(parseTag(child));
                } else if ("img".equals(childTag)) {
                    content.append(parseImage(child));
                }
            } else if (child.isTextual()) {
                content.append(child.asText());
            }
        }
        
        String itemText = content.toString().trim();
        if (itemText.isEmpty()) {
            return "";
        }
        
        // 生成 Markdown 列表项
        if (isOrdered) {
            return indent + "1. " + itemText;
        } else {
            return indent + "- " + itemText;
        }
    }
    
    /**
     * 解析链接元素
     * 
     * @param linkElement 链接元素
     * @return Markdown 链接
     */
    private String parseLink(JsonNode linkElement) {
        if (!linkElement.isArray() || linkElement.size() < 2) {
            return "";
        }
        
        String tag = linkElement.get(0).asText();
        if (!"a".equals(tag)) {
            return "";
        }
        
        JsonNode attrs = linkElement.get(1);
        String href = attrs.has("href") ? attrs.get("href").asText() : "";
        
        // 检查是否是文档引用（可能有特殊属性）
        boolean isDocRef = attrs.has("data-card-type") || 
                          attrs.has("data-doc-ref") ||
                          href.contains("/api/doc/transit");
        
        // 提取链接文本
        StringBuilder linkText = new StringBuilder();
        for (int i = 2; i < linkElement.size(); i++) {
            JsonNode child = linkElement.get(i);
            
            if (child.isArray() && child.size() > 0) {
                String childTag = child.get(0).asText();
                if ("span".equals(childTag)) {
                    linkText.append(parseTextStyle(child));
                }
            } else if (child.isTextual()) {
                linkText.append(child.asText());
            }
        }
        
        String text = linkText.toString().trim();
        
        // 如果链接文本为空或与 URL 相同，只显示 URL
        if (text.isEmpty() || text.equals(href)) {
            return href;
        }
        
        // 文档引用：保持书名号格式
        // 例如：[《Google api回传问题》](链接)
        if (isDocRef || (text.startsWith("《") && text.endsWith("》"))) {
            return "[" + text + "](" + href + ")";
        }
        
        // 普通链接：返回 Markdown 链接格式
        return "[" + text + "](" + href + ")";
    }
    
    /**
     * 解析 tag 元素（文档引用）
     * 
     * @param tagElement tag 元素
     * @return Markdown 链接
     */
    private String parseTag(JsonNode tagElement) {
        if (!tagElement.isArray() || tagElement.size() < 2) {
            return "";
        }
        
        String tag = tagElement.get(0).asText();
        if (!"tag".equals(tag)) {
            return "";
        }
        
        JsonNode attrs = tagElement.get(1);
        
        // 检查是否是文档引用类型
        String tagType = attrs.has("tagType") ? attrs.get("tagType").asText() : "";
        if (!"hetu".equals(tagType)) {
            log.debug("未知的 tag 类型: {}", tagType);
            return "";
        }
        
        // 获取 metadata
        JsonNode metadata = attrs.path("metadata");
        if (metadata.isMissingNode()) {
            log.warn("tag 元素缺少 metadata");
            return "";
        }
        
        // 获取引用的文档 ID
        String refId = metadata.has("id") ? metadata.get("id").asText() : "";
        if (refId.isEmpty()) {
            log.warn("tag 元素缺少 id");
            return "";
        }
        
        // 从 parts 中查找引用的文档信息
        if (currentParts == null || !currentParts.has(refId)) {
            log.warn("未找到引用的文档信息: {}", refId);
            return "";
        }
        
        JsonNode refPart = currentParts.get(refId);
        JsonNode refData = refPart.path("data");
        
        // 获取文档名称和 URL
        String fileName = refData.has("fileName") ? refData.get("fileName").asText() : "未命名文档";
        String metaUrl = refData.has("metaUrl") ? refData.get("metaUrl").asText() : "";
        
        if (metaUrl.isEmpty()) {
            log.warn("文档引用缺少 URL: {}", fileName);
            return "《" + fileName + "》";
        }
        
        // 返回 Markdown 链接格式，保留书名号
        return "[《" + fileName + "》](" + metaUrl + ")";
    }
    
    /**
     * 解析图片元素
     * 
     * @param imgElement 图片元素
     * @return Markdown 图片语法
     */
    private String parseImage(JsonNode imgElement) {
        if (!imgElement.isArray() || imgElement.size() < 2) {
            return "";
        }
        
        String tag = imgElement.get(0).asText();
        if (!"img".equals(tag)) {
            return "";
        }
        
        JsonNode attrs = imgElement.get(1);
        String src = attrs.has("src") ? attrs.get("src").asText() : "";
        String name = attrs.has("name") ? attrs.get("name").asText() : "图片";
        
        if (src.isEmpty()) {
            return "[图片: " + name + "]";
        }
        
        try {
            // 生成本地图片文件名
            imageCounter++;
            String extension = getImageExtension(src);
            String localImageName = String.format("image_%03d%s", imageCounter, extension);
            
            // 图片保存在 images 子目录下
            Path imagesDir = Paths.get(currentOutputDir, "images");
            
            // 确保 images 目录存在
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
                log.debug("创建 images 目录: {}", imagesDir);
            }
            
            Path imagePath = imagesDir.resolve(localImageName);
            
            // 下载图片
            log.info("下载图片: {} -> images/{}", src, localImageName);
            httpClient.downloadImage(src, currentCookie, imagePath.toString());
            
            // 返回相对路径的 Markdown 语法（images 子目录）
            return "![" + name + "](./images/" + localImageName + ")";
        } catch (Exception e) {
            log.error("下载图片失败: {}", src, e);
            // 如果下载失败，使用原始 URL
            return "![" + name + "](" + src + ")";
        }
    }
    
    /**
     * 从 URL 中获取图片扩展名
     * 
     * @param url 图片 URL
     * @return 扩展名（包含点号）
     */
    private String getImageExtension(String url) {
        if (url == null || url.isEmpty()) {
            return ".png";
        }
        
        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }
        
        // 获取扩展名
        int dotIndex = url.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < url.length() - 1) {
            String ext = url.substring(dotIndex).toLowerCase();
            // 验证是否是有效的图片扩展名
            if (ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp|svg)")) {
                return ext;
            }
        }
        
        // 默认使用 .png
        return ".png";
    }
    
    /**
     * 解析表格元素
     * 
     * @param tableElement 表格元素
     * @return Markdown 表格
     */
    private String parseTable(JsonNode tableElement) {
        if (!tableElement.isArray() || tableElement.size() < 2) {
            return "";
        }
        
        String tag = tableElement.get(0).asText();
        if (!"table".equals(tag)) {
            return "";
        }
        
        StringBuilder table = new StringBuilder();
        boolean isFirstRow = true;
        int columnCount = 0;
        
        // 遍历表格行
        for (int i = 2; i < tableElement.size(); i++) {
            JsonNode row = tableElement.get(i);
            if (!row.isArray() || row.size() < 2) {
                continue;
            }
            
            String rowTag = row.get(0).asText();
            if (!"tr".equals(rowTag)) {
                continue;
            }
            
            // 解析行
            StringBuilder rowContent = new StringBuilder("|");
            int cellCount = 0;
            
            for (int j = 2; j < row.size(); j++) {
                JsonNode cell = row.get(j);
                if (!cell.isArray() || cell.size() < 2) {
                    continue;
                }
                
                String cellTag = cell.get(0).asText();
                if (!"tc".equals(cellTag)) {
                    continue;
                }
                
                String cellContent = parseTableCell(cell);
                rowContent.append(" ").append(cellContent).append(" |");
                cellCount++;
            }
            
            if (cellCount > 0) {
                table.append(rowContent).append("\n");
                
                // 如果是第一行，添加分隔符
                if (isFirstRow) {
                    columnCount = cellCount;
                    table.append("|");
                    for (int k = 0; k < columnCount; k++) {
                        table.append(" --- |");
                    }
                    table.append("\n");
                    isFirstRow = false;
                }
            }
        }
        
        return table.toString();
    }
    
    /**
     * 解析表格单元格
     * 
     * @param cellElement 单元格元素
     * @return 单元格内容
     */
    private String parseTableCell(JsonNode cellElement) {
        if (!cellElement.isArray() || cellElement.size() < 2) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        
        for (int i = 2; i < cellElement.size(); i++) {
            JsonNode child = cellElement.get(i);
            
            if (child.isArray() && child.size() > 0) {
                String childTag = child.get(0).asText();
                if ("p".equals(childTag)) {
                    // 解析段落内容
                    for (int j = 2; j < child.size(); j++) {
                        JsonNode pChild = child.get(j);
                        if (pChild.isArray()) {
                            content.append(parseTextStyle(pChild));
                        } else if (pChild.isTextual()) {
                            content.append(pChild.asText());
                        }
                    }
                }
            }
        }
        
        String cellText = content.toString().trim();
        // 替换换行符为空格（Markdown 表格不支持多行）
        return cellText.replace("\n", " ");
    }
    
    /**
     * 解析代码块元素
     * 
     * @param codeElement 代码块元素
     * @return Markdown 代码块
     */
    private String parseCodeBlock(JsonNode codeElement) {
        if (!codeElement.isArray() || codeElement.size() < 2) {
            return "";
        }
        
        String tag = codeElement.get(0).asText();
        if (!"code".equals(tag)) {
            return "";
        }
        
        JsonNode attrs = codeElement.get(1);
        String syntax = attrs.has("syntax") ? attrs.get("syntax").asText() : "text/plain";
        String code = attrs.has("code") ? attrs.get("code").asText() : "";
        
        if (code.isEmpty()) {
            return "";
        }
        
        // 获取语言标识
        String language = CODE_LANGUAGE_MAP.getOrDefault(syntax, 
            syntax.replace("text/x-", "").replace("text/", ""));
        
        return "```" + language + "\n" + code + "\n```";
    }
    
    /**
     * 解析标题元素
     * 
     * @param headingElement 标题元素
     * @return Markdown 标题
     */
    private String parseHeading(JsonNode headingElement) {
        if (!headingElement.isArray() || headingElement.size() < 2) {
            return "";
        }
        
        String tag = headingElement.get(0).asText();
        int level = 1;
        
        // 解析标题级别（h1-h6）
        if (tag.startsWith("h") && tag.length() == 2) {
            try {
                level = Integer.parseInt(tag.substring(1));
                level = Math.max(1, Math.min(6, level)); // 限制在 1-6 之间
            } catch (NumberFormatException e) {
                level = 1;
            }
        }
        
        StringBuilder content = new StringBuilder();
        for (int i = 2; i < headingElement.size(); i++) {
            JsonNode child = headingElement.get(i);
            if (child.isTextual()) {
                content.append(child.asText());
            } else if (child.isArray()) {
                // 标题中不应用粗体样式（标题本身已经是粗体）
                content.append(parseTextStyle(child, false));
            }
        }
        
        String headingText = content.toString().trim();
        if (headingText.isEmpty()) {
            return "";
        }
        
        return "#".repeat(level) + " " + headingText;
    }
    
    /**
     * 解析列表元素
     * 
     * @param listElement 列表元素
     * @return Markdown 列表
     */
    private String parseList(JsonNode listElement) {
        if (!listElement.isArray() || listElement.size() < 2) {
            return "";
        }
        
        String tag = listElement.get(0).asText();
        boolean isOrdered = "ol".equals(tag);
        
        StringBuilder list = new StringBuilder();
        int itemNumber = 1;
        
        for (int i = 2; i < listElement.size(); i++) {
            JsonNode item = listElement.get(i);
            if (!item.isArray() || item.size() < 2) {
                continue;
            }
            
            String itemTag = item.get(0).asText();
            if (!"li".equals(itemTag)) {
                continue;
            }
            
            StringBuilder itemContent = new StringBuilder();
            for (int j = 2; j < item.size(); j++) {
                JsonNode child = item.get(j);
                if (child.isTextual()) {
                    itemContent.append(child.asText());
                } else if (child.isArray()) {
                    itemContent.append(parseTextStyle(child));
                }
            }
            
            String itemText = itemContent.toString().trim();
            if (!itemText.isEmpty()) {
                if (isOrdered) {
                    list.append(itemNumber++).append(". ").append(itemText).append("\n");
                } else {
                    list.append("- ").append(itemText).append("\n");
                }
            }
        }
        
        return list.toString();
    }
    
    /**
     * 解析引用块元素
     * 
     * @param blockquoteElement 引用块元素
     * @return Markdown 引用块
     */
    private String parseBlockquote(JsonNode blockquoteElement) {
        if (!blockquoteElement.isArray() || blockquoteElement.size() < 2) {
            return "";
        }
        
        String tag = blockquoteElement.get(0).asText();
        if (!"blockquote".equals(tag)) {
            return "";
        }
        
        StringBuilder quote = new StringBuilder();
        
        // 遍历引用块中的元素
        for (int i = 2; i < blockquoteElement.size(); i++) {
            JsonNode child = blockquoteElement.get(i);
            
            if (child.isArray() && child.size() > 0) {
                String childTag = child.get(0).asText();
                String childContent = "";
                
                switch (childTag) {
                    case "p":
                        childContent = parseParagraph(child);
                        break;
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        childContent = parseHeading(child);
                        break;
                    case "ul":
                    case "ol":
                        childContent = parseList(child);
                        break;
                    case "code":
                        childContent = parseCodeBlock(child);
                        break;
                    default:
                        childContent = parseParagraph(child);
                }
                
                if (!childContent.isEmpty()) {
                    // 为每一行添加 > 前缀
                    String[] lines = childContent.split("\n");
                    for (String line : lines) {
                        quote.append("> ").append(line).append("\n");
                    }
                }
            } else if (child.isTextual()) {
                quote.append("> ").append(child.asText()).append("\n");
            }
        }
        
        return quote.toString();
    }
}
