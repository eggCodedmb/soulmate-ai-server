package com.soulmate.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulmate.common.config.AiProperties;
import com.soulmate.common.config.JwtProperties;
import com.soulmate.common.util.JwtUtil;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import com.soulmate.service.ConversationService;
import com.soulmate.service.CompanionService;
import com.soulmate.ai.asr.AsrService;
import com.soulmate.ai.tts.TtsService;
import com.soulmate.domain.entity.CompanionVoice;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AI 通话 WebSocket 消息处理器 (Raw WebSocket 版本)
 * 处理通话过程中的语音识别文本流式对话以及打断逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallWebSocketHandler extends TextWebSocketHandler {

    private final ConversationService conversationService;
    private final CompanionService companionService;
    private final AsrService asrService;
    private final TtsService ttsService;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    /** 保存每个会话正在进行的 AI 回复订阅，用于支持打断机制 */
    private final ConcurrentHashMap<Long, Disposable> activeSubscriptions = new ConcurrentHashMap<>();

    /** 记录 sessionId 到 conversationId 的映射，用于连接关闭时清理订阅 */
    private final ConcurrentHashMap<String, Long> sessionConversationMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("AI通话 WebSocket 建立连接: sessionId={}", session.getId());

        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            log.warn("WebSocket 连接缺少查询参数: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        String token = parseToken(query);
        if (token == null) {
            log.warn("WebSocket 连接缺少 token: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        try {
            Long userId = JwtUtil.getUserId(token, jwtProperties.getSecret());
            session.getAttributes().put("userId", userId);
            log.info("WebSocket 连接认证成功: userId={}, sessionId={}", userId, session.getId());
        } catch (Exception e) {
            log.warn("WebSocket token 校验失败: sessionId={}, error={}", session.getId(), e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("未授权的 WebSocket 会话，关闭连接: sessionId={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String payload = message.getPayload();
        try {
            CallWebSocketMessage msg = objectMapper.readValue(payload, CallWebSocketMessage.class);
            log.info("收到AI通话请求: action={}, companionId={}, conversationId={}",
                    msg.getAction(), msg.getCompanionId(), msg.getConversationId());

            if ("speak".equals(msg.getAction())) {
                handleSpeak(session, userId, msg);
            } else if ("interrupt".equals(msg.getAction())) {
                handleInterrupt(session, msg);
            } else {
                log.warn("未知的操作类型: action={}", msg.getAction());
            }
        } catch (Exception e) {
            log.error("解析 WebSocket 消息失败, payload={}", payload, e);
        }
    }

    private void handleSpeak(WebSocketSession session, Long userId, CallWebSocketMessage msg) {
        Long companionId = msg.getCompanionId();
        if (companionId == null) {
            log.warn("AI通话 speak 请求参数缺失 companionId");
            return;
        }

        // 获取伴侣音色配置
        String voiceId = "mimo_default";
        try {
            CompanionVoice companionVoice = companionService.getCompanionVoice(companionId);
            if (companionVoice != null && companionVoice.getVoiceId() != null) {
                voiceId = companionVoice.getVoiceId();
            }
        } catch (Exception e) {
            log.error("获取伴侣声音配置失败: companionId={}", companionId, e);
        }

        // 1. 获取或创建 conversationId
        Long conversationId = msg.getConversationId();
        if (conversationId == null || conversationId == 0) {
            try {
                var conversation = conversationService.getOrCreateConversation(userId, companionId);
                conversationId = conversation.getId();
                log.info("自动创建/获取通话会话 ID: {}", conversationId);
            } catch (Exception e) {
                log.error("自动获取通话会话失败: userId={}, companionId={}", userId, companionId, e);
                return;
            }
        }

        // 绑定 session 与 conversationId，用于断线清理
        sessionConversationMap.put(session.getId(), conversationId);

        // 2. 如果该会话已有正在输出的流，先清理掉（打断）
        cancelSubscription(conversationId);

        // 3. 处理 ASR（如果上传了音频数据）
        String content = msg.getContent();
        if (msg.getAudio() != null && !msg.getAudio().isEmpty()) {
            try {
                byte[] audioBytes = Base64.getDecoder().decode(msg.getAudio());
                String transcribed = asrService.transcribe(audioBytes, "voice.wav");
                if (transcribed == null || transcribed.isBlank()) {
                    log.warn("ASR 识别结果为空，忽略并通知客户端重听");
                    if (session.isOpen()) {
                        Map<String, Object> respMap = Map.of(
                                "action", "speak",
                                "conversationId", conversationId,
                                "done", true
                        );
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(respMap)));
                    }
                    return;
                }
                content = transcribed;
                log.info("ASR 识别成功: {}", content);
            } catch (Exception e) {
                log.error("ASR 处理失败", e);
                return;
            }
        }

        if (content == null || content.isEmpty()) {
            log.warn("AI通话 content 参数缺失且无音频数据");
            return;
        }

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setConversationId(conversationId);
        chatRequest.setCompanionId(companionId);
        chatRequest.setContent(content);
        chatRequest.setContentType("text");
        chatRequest.setLlmModel(aiProperties.getCallModel());
        chatRequest.setSceneMode("voice_call");

        // 4. 订阅流式回复 (开场白指令时走专门的 sendGreeting，避免数据库记录该指令)
        Flux<ChatResponse> responseFlux = "[GREETING]".equals(content)
                ? conversationService.sendGreeting(userId, chatRequest)
                : conversationService.sendMessage(userId, chatRequest);
        final Long finalConversationId = conversationId;
        final String finalVoiceId = voiceId;

        SentenceSplitter splitter = new SentenceSplitter();
        
        Flux<String> sentenceFlux = responseFlux
                .publishOn(Schedulers.boundedElastic())
                .flatMap(chatResponse -> {
                    if (chatResponse.getError() != null) {
                        return Flux.error(new RuntimeException(chatResponse.getError()));
                    }
                    List<String> sentences = splitter.feed(chatResponse.getContent() != null ? chatResponse.getContent() : "");
                    return Flux.fromIterable(sentences);
                });

        sentenceFlux = sentenceFlux.concatWith(Mono.defer(() -> {
            String remaining = splitter.getRemaining();
            if (!remaining.isEmpty()) {
                return Mono.just(remaining);
            }
            return Mono.empty();
        }));

        Disposable subscription = sentenceFlux
                .concatMap(sentence -> Mono.fromCallable(() -> {
                    try {
                        if (session.isOpen()) {
                            log.info("Generating TTS for sentence: {}", sentence);
                            byte[] audioBytes = ttsService.generateTts(sentence, finalVoiceId);
                            if (audioBytes != null && audioBytes.length > 0) {
                                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                                Map<String, Object> respMap = new HashMap<>();
                                respMap.put("action", "speak");
                                respMap.put("conversationId", finalConversationId);
                                respMap.put("content", sentence);
                                respMap.put("audio", base64Audio);
                                respMap.put("done", false);
                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(respMap)));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to generate/send TTS for sentence: {}", sentence, e);
                    }
                    return sentence;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(error -> {
                    log.error("AI通话流生成异常: conversationId={}", finalConversationId, error);
                    try {
                        if (session.isOpen()) {
                            Map<String, Object> errMap = Map.of(
                                    "action", "speak",
                                    "conversationId", finalConversationId,
                                    "error", "服务出现了一些问题，请稍后再试",
                                    "done", true
                            );
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errMap)));
                        }
                    } catch (IOException e) {
                        log.error("推送异常消息失败", e);
                    }
                    activeSubscriptions.remove(finalConversationId);
                })
                .doOnComplete(() -> {
                    log.info("AI通话流推送完成: conversationId={}", finalConversationId);
                    try {
                        if (session.isOpen()) {
                            Map<String, Object> respMap = new HashMap<>();
                            respMap.put("action", "speak");
                            respMap.put("conversationId", finalConversationId);
                            respMap.put("done", true);
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(respMap)));
                        }
                    } catch (Exception e) {
                        log.error("Failed to send final done message", e);
                    }
                    activeSubscriptions.remove(finalConversationId);
                })
                .subscribe();

        // 5. 记录当前活跃订阅
        activeSubscriptions.put(conversationId, subscription);
    }

    private void handleInterrupt(WebSocketSession session, CallWebSocketMessage msg) {
        Long conversationId = msg.getConversationId();
        log.info("收到 AI 通话打断信号: conversationId={}", conversationId);

        if (conversationId != null) {
            boolean cancelled = cancelSubscription(conversationId);
            if (cancelled) {
                log.info("成功终止/打断流式响应: conversationId={}", conversationId);
            }
            try {
                if (session.isOpen()) {
                    Map<String, Object> ackMap = Map.of(
                            "action", "interrupt-ack",
                            "conversationId", conversationId,
                            "success", true
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackMap)));
                }
            } catch (IOException e) {
                log.error("发送打断 ACK 异常", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("AI通话 WebSocket 关闭连接: sessionId={}, status={}", session.getId(), status);
        Long conversationId = sessionConversationMap.remove(session.getId());
        if (conversationId != null) {
            cancelSubscription(conversationId);
        }
    }

    /**
     * 取消指定会话的流式订阅
     */
    private boolean cancelSubscription(Long conversationId) {
        Disposable subscription = activeSubscriptions.remove(conversationId);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            return true;
        }
        return false;
    }

    private String parseToken(String query) {
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }

    @Data
    private static class CallWebSocketMessage {
        private String action;
        private Long companionId;
        private Long conversationId;
        private String content;
        private String audio;
    }

    private static class SentenceSplitter {
        private final StringBuilder buffer = new StringBuilder();
        private static final Pattern PUNCTUATION = Pattern.compile("[。！？\\.\\?!;\\n]");

        public List<String> feed(String token) {
            List<String> sentences = new ArrayList<>();
            buffer.append(token);
            String text = buffer.toString();
            
            Matcher matcher = PUNCTUATION.matcher(text);
            int lastMatchEnd = 0;
            while (matcher.find()) {
                int end = matcher.end();
                String sentence = text.substring(lastMatchEnd, end).trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                lastMatchEnd = end;
            }
            
            if (lastMatchEnd > 0) {
                buffer.setLength(0);
                if (lastMatchEnd < text.length()) {
                    buffer.append(text.substring(lastMatchEnd));
                }
            }
            return sentences;
        }

        public String getRemaining() {
            String remaining = buffer.toString().trim();
            buffer.setLength(0);
            return remaining;
        }
    }
}
