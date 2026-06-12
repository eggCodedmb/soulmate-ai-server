package com.soulmate.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 编辑伴侣请求（所有字段可选，支持局部更新）
 */
@Data
public class UpdateCompanionRequest {

    @Size(max = 64, message = "名字不能超过64个字符")
    private String name;

    private Integer gender;

    private String relationshipType;

    /** 性格标签（最多3个，传入则整体替换，不传则不更新） */
    @Size(max = 3, message = "性格标签最多选择3个")
    private List<String> personalityKeys;

    /** 说话风格 */
    private String speakingStyle;

    /** 出生日期 */
    private LocalDate birthday;

    /** 背景故事 */
    private String description;
}
