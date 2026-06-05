package com.soulmate.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 关系类型枚举
 */
@Getter
@AllArgsConstructor
public enum RelationshipType {

    LOVER("lover", "恋人"),
    FRIEND("friend", "挚友"),
    MENTOR("mentor", "导师"),
    CONFIDANT("confidant", "树洞");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
