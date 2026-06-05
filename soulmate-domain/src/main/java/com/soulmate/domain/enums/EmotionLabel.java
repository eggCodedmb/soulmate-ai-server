package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 情绪标签枚举
 */
@Getter
@AllArgsConstructor
public enum EmotionLabel {

    HAPPY("happy", "开心"),
    SAD("sad", "难过"),
    ANXIOUS("anxious", "焦虑"),
    ANGRY("angry", "愤怒"),
    LONELY("lonely", "孤独"),
    CALM("calm", "平静"),
    EXCITED("excited", "兴奋");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
