package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
@AllArgsConstructor
public enum Gender {

    UNSET(0, "未设置"),
    MALE(1, "男"),
    FEMALE(2, "女"),
    NON_BINARY(3, "非二元");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;
}
