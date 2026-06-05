package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性格特征枚举
 */
@Getter
@AllArgsConstructor
public enum PersonalityKey {

    GENTLE("gentle", "温柔"),
    LIVELY("lively", "活泼"),
    CALM("calm", "沉稳"),
    HUMOROUS("humorous", "幽默"),
    INTELLECTUAL("intellectual", "知性"),
    COOL("cool", "高冷");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
