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

    FORMAL("formal", "正式礼貌"),
    CASUAL("casual", "日常口语"),
    CUTE("cute", "软萌可爱"),
    COOL("cool", "简洁冷酷"),
    HUMOROUS("humorous", "幽默风趣"),
    POETIC("poetic", "文艺诗意"),
    // 以下为旧值，保持向后兼容
    LITERARY("literary", "文艺"),
    FUNNY("funny", "搞笑");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
