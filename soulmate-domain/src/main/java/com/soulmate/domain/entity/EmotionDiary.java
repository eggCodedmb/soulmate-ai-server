package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记表
 */
@Data
@TableName("t_emotion_diary")
public class EmotionDiary {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 日记日期 */
    private LocalDate diaryDate;

    /** 当日整体情绪 */
    private String overallEmotion;

    /** 当日情绪均分 */
    private BigDecimal avgScore;

    /** AI生成的当日情绪摘要 */
    private String summary;

    /** 当日关键词 */
    private String keyword;

    /** 当日对话数 */
    private Integer conversationCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
