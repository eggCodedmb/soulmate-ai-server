package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {

    PROACTIVE_CARE("proactive_care", "主动关心"),
    BIRTHDAY("birthday", "生日祝福"),
    GREETING("greeting", "问候"),
    SYSTEM("system", "系统通知"),
    SUBSCRIPTION("subscription", "订阅通知");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
