package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对话场景模式
 */
@Getter
@AllArgsConstructor
public enum SceneMode {

    DAILY("daily", "日常聊天"),
    DEEP_NIGHT("deep_night", "深夜倾诉"),
    STORY("story", "故事共创"),
    ROLEPLAY("roleplay", "角色扮演"),
    STUDY("study", "学习陪伴"),
    GREETING("greeting", "早安/晚安");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
