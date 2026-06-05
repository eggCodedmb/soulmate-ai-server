package com.soulmate.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 聊天响应（用于SSE流式输出）
 */
@Data
@Builder
public class ChatResponse {

    /** 消息ID */
    private Long messageId;

    /** 会话ID */
    private Long conversationId;

    /** 内容片段（流式） */
    private String content;

    /** 是否完成 */
    private boolean done;

    /** 情绪标签 */
    private String emotionTag;

    /** 错误信息 */
    private String error;

    /** token使用量 */
    private Integer tokensUsed;
}
