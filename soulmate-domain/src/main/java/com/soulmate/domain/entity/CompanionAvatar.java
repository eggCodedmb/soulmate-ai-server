package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.AvatarType;
import com.soulmate.domain.enums.Expression;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伴侣形象配置表
 */
@Data
@TableName("t_companion_avatar")
public class CompanionAvatar {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 伴侣ID */
    private Long companionId;

    /** 类型：preset/ai_generated/uploaded */
    private AvatarType avatarType;

    /** 形象图片URL */
    private String imageUrl;

    /** 当前表情 */
    private Expression expression;

    /** Lottie动画文件URL */
    private String lottieUrl;

    /** Stable Diffusion 生成提示词 */
    private String sdPrompt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
