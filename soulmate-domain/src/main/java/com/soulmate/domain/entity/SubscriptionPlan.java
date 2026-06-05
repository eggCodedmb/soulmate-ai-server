package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐定义表
 */
@Data
@TableName("t_subscription_plan")
public class SubscriptionPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 套餐编码：FREE/BASIC/PREMIUM/ULTIMATE */
    private String planCode;

    /** 套餐名称 */
    private String planName;

    /** 月费（元） */
    private BigDecimal priceMonthly;

    /** 最大伴侣数（-1=无限） */
    private Integer maxCompanions;

    /** 每日消息上限（-1=无限） */
    private Integer maxDailyMessages;

    /** 语音消息：0-否 1-是 */
    private Integer voiceMessage;

    /** 语音通话：0-否 1-是 */
    private Integer voiceCall;

    /** 高级记忆：0-否 1-是 */
    private Integer advancedMemory;

    /** 自定义声音：0-否 1-是 */
    private Integer customVoice;

    /** 优先响应：0-否 1-是 */
    private Integer priorityResponse;

    /** 展示排序 */
    private Integer displayOrder;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
