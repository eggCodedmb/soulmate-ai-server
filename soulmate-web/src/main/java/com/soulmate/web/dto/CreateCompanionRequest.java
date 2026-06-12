package com.soulmate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建伴侣请求
 */
@Data
public class CreateCompanionRequest {

    @NotBlank(message = "伴侣名字不能为空")
    @Size(max = 64, message = "名字不能超过64个字符")
    private String name;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @NotBlank(message = "关系类型不能为空")
    private String relationshipType;

    /** 性格标签（最多3个） */
    @Size(max = 3, message = "性格标签最多选择3个")
    private List<String> personalityKeys;

    /** 说话风格 */
    private String speakingStyle = "casual";

    /** 出生日期 */
    private LocalDate birthday;

    /** 背景故事 */
    private String description;
}
