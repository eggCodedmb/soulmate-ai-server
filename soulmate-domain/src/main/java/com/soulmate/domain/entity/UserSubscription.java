package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.SubscriptionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅表
 */
@Data
@TableName("t_user_subscription")
public class UserSubscription {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 套餐ID */
    private Long planId;

    /** 订阅开始时间 */
    private LocalDateTime startTime;

    /** 订阅到期时间 */
    private LocalDateTime endTime;

    /** 自动续费：0-否 1-是 */
    private Integer autoRenew;

    /** 状态 */
    private SubscriptionStatus status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
