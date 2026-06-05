package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 伴侣形象类型枚举
 */
@Getter
@AllArgsConstructor
public enum AvatarType {

    PRESET("preset", "预设"),
    AI_GENERATED("ai_generated", "AI生成"),
    UPLOADED("uploaded", "上传");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
