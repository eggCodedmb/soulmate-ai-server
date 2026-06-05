package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 伴侣表情枚举
 */
@Getter
@AllArgsConstructor
public enum Expression {

    NORMAL("normal", "常态"),
    HAPPY("happy", "开心"),
    SHY("shy", "害羞"),
    THINKING("thinking", "思考"),
    SAD("sad", "难过");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
