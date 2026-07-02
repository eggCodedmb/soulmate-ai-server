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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 对话控制器
 */
@Slf4j
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
    public ResponseEntity<SseEmitter> streamChat(@RequestAttribute("currentUserId") Long userId,
                                                 @Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        Flux<ChatResponse> flux = conversationService.sendMessage(userId, request);

        flux.subscribe(
            chatResponse -> {
                try {
                    emitter.send(chatResponse);
                } catch (Exception e) {
                    log.warn("SSE发送失败: userId={}, error={}", userId, e.getMessage());
                }
            },
            error -> {
                emitter.completeWithError(error);
            },
            () -> {
                emitter.complete();
            }
        );

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    /**
     * 发送消息（同步AI回复）
     */
    @PostMapping("/chat/send")
    public R<Message> sendMessage(@RequestAttribute("currentUserId") Long userId,
                                   @Valid @RequestBody ChatRequest request) {
        return R.ok(conversationService.sendMessageSync(userId, request));
    }

    /**
     * 删除单条消息
     */
    @DeleteMapping("/message/{id}")
    public R<Void> deleteMessage(@RequestAttribute("currentUserId") Long userId,
                                  @PathVariable Long id) {
        conversationService.deleteMessage(userId, id);
        return R.ok();
    }
}

