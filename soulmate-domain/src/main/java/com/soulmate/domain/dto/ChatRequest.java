package com.soulmate.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 聊天请求
 */
@Data
public class ChatRequest {

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /** 伴侣ID */
    @NotNull(message = "伴侣ID不能为空")
    private Long companionId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 内容类型：text/voice/image，默认 text */
    private String contentType = "text";

    /** 场景模式（可选，切换场景时传入） */
    private String sceneMode;
}
