package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单表
 */
@Data
@TableName("t_payment_order")
public class PaymentOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 套餐ID */
    private Long planId;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 支付渠道 */
    private String paymentChannel;

    /** 支付状态 */
    private PaymentStatus paymentStatus;

    /** 第三方交易号（支付宝） */
    private String tradeNo;

    /** 支付完成时间 */
    private LocalDateTime paidTime;

    /** 周期类型 */
    private String periodType;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
