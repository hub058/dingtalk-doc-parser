package com.dingtalk.doc.model;

import lombok.Data;

/**
 * 钉钉用户信息
 * 
 * @author DingTalk Doc Parser Team
 */
@Data
public class DingTalkUser {
    
    /**
     * 用户的userId
     */
    private String userid;
    
    /**
     * 用户的unionid
     */
    private String unionid;
    
    /**
     * 用户名称
     */
    private String name;
    
    /**
     * 手机号
     */
    private String mobile;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 职位
     */
    private String title;
    
    /**
     * 工号
     */
    private String jobNumber;
    
    /**
     * 部门ID列表
     */
    private Long[] deptIdList;
    
    /**
     * 是否激活
     */
    private Boolean active;
    
    /**
     * 是否管理员
     */
    private Boolean admin;
    
    /**
     * 是否高管
     */
    private Boolean boss;
    
    /**
     * 入职时间
     */
    private Long hiredDate;
    
    /**
     * 备注
     */
    private String remark;
}
