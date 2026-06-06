package com.soulmate.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建支付请求
 */
@Data
public class CreatePaymentRequest {

    /** 套餐ID */
    @NotNull(message = "套餐ID不能为空")
    private Long planId;
}
