package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.ContentType;
import com.soulmate.domain.enums.SenderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消息表
 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 发送方：user/companion */
    private SenderType senderType;

    /** 消息内容 */
    private String content;

    /** 内容类型：text/voice/image/system */
    private ContentType contentType;

    /** 语音文件URL */
    private String voiceUrl;

    /** 语音时长（秒） */
    private Integer voiceDuration;

    /** 图片URL */
    private String imageUrl;

    /** AI识别的情绪标签 */
    private String emotionTag;

    /** 情绪得分：-1.00 ~ 1.00 */
    private BigDecimal emotionScore;

    /** 本次消耗的token数 */
    private Integer tokensUsed;

    /** 生成该消息的模型标识 */
    private String llmModel;

    /** 已读状态：0-未读 1-已读 */
    private Integer readStatus;

    /** 发送时间 */
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
