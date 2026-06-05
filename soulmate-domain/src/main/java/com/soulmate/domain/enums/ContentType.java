package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息内容类型
 */
@Getter
@AllArgsConstructor
public enum ContentType {

    TEXT("text", "文字"),
    VOICE("voice", "语音"),
    IMAGE("image", "图片"),
    SYSTEM("system", "系统消息");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
