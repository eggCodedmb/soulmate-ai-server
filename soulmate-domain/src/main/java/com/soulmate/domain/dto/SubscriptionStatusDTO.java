package com.soulmate.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionStatusDTO {
    /**
     * 套餐代码 (例如 FREE, BASIC, PREMIUM)
     */
    private String planCode;

    /**
     * 套餐名称 (例如 免费版, 基础版)
     */
    private String planName;

    /**
     * 每日最大消息数 (-1 表示无限制)
     */
    private Integer maxDailyMessages;

    /**
     * 今日已发送消息数
     */
    private Integer todayUsedMessages;

    /**
     * 今日剩余消息数 (-1 表示无限制)
     */
    private Integer remainingMessages;

    /**
     * 最大伴侣数 (-1 表示无限制)
     */
    private Integer maxCompanions;

    /**
     * 当前已创建的活跃伴侣数
     */
    private Integer currentCompanions;

    /**
     * 套餐到期时间 (免费版为空)
     */
    private LocalDateTime expireTime;
}
