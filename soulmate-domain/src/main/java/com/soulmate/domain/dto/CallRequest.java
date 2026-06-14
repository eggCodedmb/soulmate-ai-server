package com.soulmate.domain.dto;

import lombok.Data;

/**
 * AI通话请求数据
 */
@Data
public class CallRequest {
    
    /** 用户ID */
    private Long userId;

    /** 会话ID */
    private Long conversationId;

    /** 伴侣ID */
    private Long companionId;

    /** 语音转成的文本内容 */
    private String content;
}
