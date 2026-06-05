package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 伴侣声音配置表
 */
@Data
@TableName("t_companion_voice")
public class CompanionVoice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 伴侣ID */
    private Long companionId;

    /** 音色标识（Azure TTS voice name） */
    private String voiceId;

    /** 音色显示名 */
    private String voiceName;

    /** 音调偏移 */
    private BigDecimal pitch;

    /** 语速倍率 */
    private BigDecimal speed;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
