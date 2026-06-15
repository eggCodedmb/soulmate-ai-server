package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记忆分类枚举
 */
@Getter
@AllArgsConstructor
public enum MemoryCategory {

    PERSONAL_INFO("personal_info", "个人信息"),
    SHARED_EXPERIENCE("shared_experience", "共同经历"),
    PREFERENCE("preference", "偏好习惯"),
    HABIT("habit", "日常习惯"),
    PRIVATE_PREFERENCE("private_preference", "私密爱好");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
