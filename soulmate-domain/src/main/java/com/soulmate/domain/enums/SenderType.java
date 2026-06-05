package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息发送方类型
 */
@Getter
@AllArgsConstructor
public enum SenderType {

    USER("user", "用户"),
    COMPANION("companion", "伴侣");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
