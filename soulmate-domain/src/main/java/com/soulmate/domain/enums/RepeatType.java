package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提醒重复类型枚举
 */
@Getter
@AllArgsConstructor
public enum RepeatType {

    ONCE("once", "单次"),
    DAILY("daily", "每天"),
    WEEKLY("weekly", "每周"),
    MONTHLY("monthly", "每月");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
