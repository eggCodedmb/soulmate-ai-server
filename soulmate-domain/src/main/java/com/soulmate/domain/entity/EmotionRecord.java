package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.EmotionLabel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消息级情绪记录
 */
@Data
@TableName("t_emotion_record")
public class EmotionRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联消息ID */
    private Long messageId;

    /** 情绪标签 */
    private EmotionLabel emotionLabel;

    /** 情绪得分：-1.00 ~ 1.00 */
    private BigDecimal emotionScore;

    /** 情绪强度：1-5 */
    private Integer intensity;

    /** 检测方式：llm/bert */
    private String detectionMethod;

    private LocalDateTime createTime;
}
