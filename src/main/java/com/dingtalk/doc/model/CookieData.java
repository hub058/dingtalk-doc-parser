package com.dingtalk.doc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cookie 数据模型
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieData {
    
    /**
     * Cookie 列表
     */
    private List<Cookie> cookies;
    
    /**
     * Cookie 字符串（格式：name1=value1; name2=value2）
     */
    private String cookieString;
    
    /**
     * 保存时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 获取模式（auto_login 或 manual）
     */
    private String mode;
}
