package com.soulmate.web.controller;

import com.soulmate.domain.dto.CallRequest;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import com.soulmate.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 通话 WebSocket 消息控制器
 * 处理通话过程中的语音识别文本流式对话以及打断逻辑
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CallWebSocketController {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    /** 保存每个会话正在进行的 AI 回复订阅，用于支持打断机制 */
    private final ConcurrentHashMap<Long, Disposable> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * 接收用户语音文本，调用 LLM 并流式推送 AI 回复
     */
    @MessageMapping("/call/speak")
    public void handleSpeak(CallRequest callRequest) {
        Long conversationId = callRequest.getConversationId();
        log.info("收到AI通话用户输入: userId={}, conversationId={}, content={}",
                callRequest.getUserId(), conversationId, callRequest.getContent());

        if (callRequest.getUserId() == null || conversationId == null || callRequest.getCompanionId() == null) {
            log.warn("AI通话请求参数缺失: {}", callRequest);
            return;
        }

        // 1. 如果该会话已有正在输出的流，先清理掉（安全防范，通常由打断指令触发）
        cancelSubscription(conversationId);

        // 2. 构建 ChatRequest 并调用对话服务
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setConversationId(conversationId);
        chatRequest.setCompanionId(callRequest.getCompanionId());
        chatRequest.setContent(callRequest.getContent());
        chatRequest.setContentType("text");

        // 3. 订阅流式回复
        Flux<ChatResponse> responseFlux = conversationService.sendMessage(callRequest.getUserId(), chatRequest);

        String destination = "/topic/call/" + conversationId;
        Disposable subscription = responseFlux.subscribe(
                chatResponse -> {
                    // 推送实时文本 Token
                    messagingTemplate.convertAndSend(destination, chatResponse);
                },
                error -> {
                    log.error("AI通话流生成异常: conversationId={}", conversationId, error);
                    messagingTemplate.convertAndSend(destination, ChatResponse.builder()
                            .error("服务出现了一些问题，请稍后再试")
                            .done(true)
                            .build());
                    activeSubscriptions.remove(conversationId);
                },
                () -> {
                    // 完成后清除订阅
                    log.info("AI通话流推送完成: conversationId={}", conversationId);
                    activeSubscriptions.remove(conversationId);
                }
        );

        // 4. 记录当前活跃订阅
        activeSubscriptions.put(conversationId, subscription);
    }

    /**
     * 接收客户端的打断指令，取消当前正在运行的大模型推理流
     */
    @MessageMapping("/call/interrupt")
    public void handleInterrupt(CallRequest callRequest) {
        Long conversationId = callRequest.getConversationId();
        log.info("收到AI通话打断信号: conversationId={}", conversationId);
        
        if (conversationId != null) {
            boolean cancelled = cancelSubscription(conversationId);
            if (cancelled) {
                log.info("成功终止/打断流式响应: conversationId={}", conversationId);
            }
            // 回送打断确认通知客户端
            messagingTemplate.convertAndSend("/topic/call/" + conversationId + "/interrupt-ack", (Object) Map.of("success", true));
        }
    }

    /**
     * 取消指定会话的流式订阅
     * @return 是否有订阅被取消
     */
    private boolean cancelSubscription(Long conversationId) {
        Disposable subscription = activeSubscriptions.remove(conversationId);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            return true;
        }
        return false;
    }
}
