package com.soulmate.web.controller;

import com.soulmate.common.response.PageResult;
import com.soulmate.common.response.R;
import com.soulmate.domain.entity.Conversation;
import com.soulmate.domain.entity.Message;
import com.soulmate.service.ConversationService;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 对话控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 创建或获取会话
     */
    @PostMapping("/conversation")
    public R<Conversation> getOrCreateConversation(@RequestAttribute("currentUserId") Long userId,
                                                    @RequestParam Long companionId) {
        return R.ok(conversationService.getOrCreateConversation(userId, companionId));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversation/list")
    public R<List<Conversation>> getConversations(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(conversationService.getUserConversations(userId));
    }

    /**
     * 获取历史消息（分页）
     */
    @GetMapping("/conversation/{id}/messages")
    public R<PageResult<Message>> getHistoryMessages(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return R.ok(conversationService.getHistoryMessages(id, page, size));
    }

    /**
     * 发送消息（SSE流式AI回复）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestAttribute("currentUserId") Long userId,
                                          @Valid @RequestBody ChatRequest request,
                                          jakarta.servlet.http.HttpServletResponse response) {
        // 显式禁用缓冲，确保逐字输出
        response.setHeader("X-Accel-Buffering", "no");
        return conversationService.sendMessage(userId, request);
    }

    /**
     * 发送消息（同步AI回复）
     */
    @PostMapping("/chat/send")
    public R<Message> sendMessage(@RequestAttribute("currentUserId") Long userId,
                                   @Valid @RequestBody ChatRequest request) {
        return R.ok(conversationService.sendMessageSync(userId, request));
    }
}
