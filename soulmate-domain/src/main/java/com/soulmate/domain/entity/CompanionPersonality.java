package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.PersonalityKey;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伴侣性格标签表
 */
@Data
@TableName("t_companion_personality")
public class CompanionPersonality {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 伴侣ID */
    private Long companionId;

    /** 性格关键词 */
    private PersonalityKey personalityKey;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
