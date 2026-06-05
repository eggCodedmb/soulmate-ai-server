package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 说话风格枚举
 */
@Getter
@AllArgsConstructor
public enum SpeakingStyle {

    FORMAL("formal", "正式"),
    CASUAL("casual", "随意"),
    LITERARY("literary", "文艺"),
    FUNNY("funny", "搞笑");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
