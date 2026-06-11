package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.PageResult;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.entity.*;
import com.soulmate.domain.enums.*;
import com.soulmate.mapper.*;
import com.soulmate.service.ConversationService;
import com.soulmate.service.SubscriptionService;
import com.soulmate.service.ChatService;
import com.soulmate.service.MemoryService;
import com.soulmate.service.CompanionReminderService;
import com.soulmate.domain.dto.ChatRequest;
import com.soulmate.domain.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static com.soulmate.common.constant.RedisConstants.COMPANION_CONTEXT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final CompanionMapper companionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ChatService chatService;
    private final SubscriptionService subscriptionService;
    private final MemoryService memoryService;
    private final CompanionReminderService companionReminderService;

    @Override
    @Transactional
    public Conversation getOrCreateConversation(Long userId, Long companionId) {
        Conversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .eq(Conversation::getCompanionId, companionId)
                        .eq(Conversation::getDeleted, 0));
        if (existing != null) {
            Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, existing.getId())
                    .eq(Message::getDeleted, 0));
            existing.setMessageCount(count.intValue());
            return existing;
        }

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setCompanionId(companionId);
        conversation.setSceneMode(SceneMode.DAILY);
        conversation.setUnreadCount(0);
        conversation.setPinned(0);
        conversation.setContextWindow(50);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        conversation.setMessageCount(0);
        return conversation;
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        List<Conversation> list = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .eq(Conversation::getDeleted, 0)
                        .orderByDesc(Conversation::getPinned)
                        .orderByDesc(Conversation::getLastMessageTime));

        for (Conversation conv : list) {
            Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conv.getId())
                    .eq(Message::getDeleted, 0));
            conv.setMessageCount(count.intValue());
        }
        return list;
    }

    @Override
    public PageResult<Message> getHistoryMessages(Long conversationId, int page, int size) {
        Page<Message> pageResult = messageMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .eq(Message::getDeleted, 0)
                        .orderByDesc(Message::getCreateTime));
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    @Override
    @Transactional
    public Flux<ChatResponse> sendMessage(Long userId, ChatRequest request) {
        // 检查消息限制
        if (!subscriptionService.checkDailyMessageLimit(userId)) {
            return Flux.just(ChatResponse.builder()
                    .error(ResultCode.DAILY_MESSAGE_LIMIT.getMessage())
                    .done(true)
                    .build());
        }

        Conversation conversation = conversationMapper.selectById(request.getConversationId());
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return Flux.just(ChatResponse.builder()
                    .error(ResultCode.CONVERSATION_NOT_FOUND.getMessage())
                    .done(true)
                    .build());
        }

        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setSenderType(SenderType.USER);
        userMessage.setContent(request.getContent());
        userMessage.setContentType(ContentType.valueOf(request.getContentType().toUpperCase()));
        userMessage.setReadStatus(1);
        userMessage.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMessage);

        // 更新会话
        conversation.setLastMessagePreview(
                request.getContent().length() > 100
                        ? request.getContent().substring(0, 100) + "..."
                        : request.getContent());
        conversation.setLastMessageTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        // 增加消息计数
        subscriptionService.incrementDailyMessageCount(userId);

        // 获取伴侣信息
        Companion companion = companionMapper.selectById(request.getCompanionId());

        StringBuilder fullContent = new StringBuilder();

        // 调用 AI 服务流式生成回复
        return chatService.streamChat(userId, conversation, companion, request.getContent(), request)
                .doOnNext(res -> {
                    if (res.getContent() != null) {
                        fullContent.append(res.getContent());
                    }
                })
                .doOnComplete(() -> {
                    String aiReply = fullContent.toString();
                    if (!aiReply.isBlank()) {
                        // 1. 解析并自动创建定时提醒
                        companionReminderService.parseAndCreateReminder(userId, companion.getId(), aiReply);

                        // 2. 清洗过滤控制指令标签和 tool call 残留
                        String cleanReply = aiReply
                                .replaceAll("<command.*?>.*?</command>", "")
                                .replaceAll("(?s)<tool_call>.*?</tool_call>", "")
                                .replaceAll("(?s)<function=.*?</function>", "")
                                .replaceAll("(?s)<query [^>]*>.*?</query>", "")
                                .trim();

                        // 保存AI回复消息
                        Message aiMessage = new Message();
                        aiMessage.setConversationId(conversation.getId());
                        aiMessage.setSenderType(SenderType.COMPANION);
                        aiMessage.setContent(cleanReply);
                        aiMessage.setContentType(ContentType.TEXT);
                        aiMessage.setReadStatus(0);
                        aiMessage.setCreateTime(LocalDateTime.now());
                        messageMapper.insert(aiMessage);

                        // 保存assistant回复到Redis上下文
                        try {
                            String ctxKey = COMPANION_CONTEXT + conversation.getId();
                            redisTemplate.opsForList().rightPush(ctxKey, "assistant:" + cleanReply);
                            redisTemplate.opsForList().trim(ctxKey, -(conversation.getContextWindow() * 2L), -1);
                        } catch (Exception e) {
                            log.warn("保存assistant上下文失败: conversationId={}", conversation.getId(), e);
                        }

                        // 更新会话最后消息
                        conversation.setLastMessagePreview(
                                cleanReply.length() > 100 ? cleanReply.substring(0, 100) + "..." : cleanReply);
                        conversation.setLastMessageTime(LocalDateTime.now());
                        conversationMapper.updateById(conversation);

                        // 异步提取记忆
                        memoryService.extractMemories(userId, companion.getId(), conversation.getId());
                        log.info("AI回复并保存成功: conversationId={}", conversation.getId());
                    }
                });
    }

    @Override
    @Transactional
    public Message sendMessageSync(Long userId, ChatRequest request) {
        Conversation conversation = conversationMapper.selectById(request.getConversationId());
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BizException(ResultCode.CONVERSATION_NOT_FOUND);
        }

        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setSenderType(SenderType.USER);
        userMessage.setContent(request.getContent());
        userMessage.setContentType(ContentType.TEXT);
        userMessage.setReadStatus(1);
        userMessage.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMessage);

        // 获取伴侣信息并调用AI
        Companion companion = companionMapper.selectById(request.getCompanionId());
        String aiReply = chatService.chatSync(userId, conversation, companion, request.getContent(), request);

        // 1. 解析并自动创建定时提醒
        companionReminderService.parseAndCreateReminder(userId, companion.getId(), aiReply);

        // 2. 清洗过滤控制指令标签和 tool call 残留
        String cleanReply = aiReply
                .replaceAll("<command.*?>.*?</command>", "")
                .replaceAll("(?s)<tool_call>.*?</tool_call>", "")
                .replaceAll("(?s)<function=.*?</function>", "")
                .replaceAll("(?s)<query [^>]*>.*?</query>", "")
                .trim();

        // 保存AI回复
        Message aiMessage = new Message();
        aiMessage.setConversationId(conversation.getId());
        aiMessage.setSenderType(SenderType.COMPANION);
        aiMessage.setContent(cleanReply);
        aiMessage.setContentType(ContentType.TEXT);
        aiMessage.setReadStatus(0);
        aiMessage.setCreateTime(LocalDateTime.now());
        messageMapper.insert(aiMessage);

        // 保存assistant回复到Redis上下文
        try {
            String ctxKey = COMPANION_CONTEXT + conversation.getId();
            redisTemplate.opsForList().rightPush(ctxKey, "assistant:" + cleanReply);
            redisTemplate.opsForList().trim(ctxKey, -(conversation.getContextWindow() * 2L), -1);
        } catch (Exception e) {
            log.warn("保存assistant上下文失败: conversationId={}", conversation.getId(), e);
        }

        // 更新会话
        conversation.setLastMessagePreview(
                cleanReply.length() > 100 ? cleanReply.substring(0, 100) + "..." : cleanReply);
        conversation.setLastMessageTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        // 异步提取记忆
        memoryService.extractMemories(userId, companion.getId(), conversation.getId());

        return aiMessage;
    }
}
