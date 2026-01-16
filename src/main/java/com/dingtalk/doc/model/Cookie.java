package com.dingtalk.doc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cookie 模型
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cookie {
    
    /**
     * Cookie 名称
     */
    private String name;
    
    /**
     * Cookie 值
     */
    private String value;
    
    /**
     * Cookie 域名
     */
    private String domain;
    
    /**
     * Cookie 路径
     */
    private String path;
    
    /**
     * 过期时间（Unix 时间戳）
     */
    private Long expires;
}
