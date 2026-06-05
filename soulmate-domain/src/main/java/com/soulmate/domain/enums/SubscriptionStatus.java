package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅状态枚举
 */
@Getter
@AllArgsConstructor
public enum SubscriptionStatus {

    CANCELLED(0, "已取消"),
    ACTIVE(1, "生效中"),
    EXPIRED(2, "已过期");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;
}
