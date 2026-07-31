package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料表
 */
@Data
@TableName("t_user_profile")
public class UserProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 性格测试结果（如 MBTI: INFP） */
    private String personalityType;

    /** 性格问卷原始答案（JSON字符串） */
    private String personalityResult;

    /** 兴趣标签（JSON字符串） */
    private String interests;

    /** 偏好聊天风格 */
    private String chatStylePref;

    /** 禁忌话题列表（JSON字符串） */
    private String topicsBlacklist;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
