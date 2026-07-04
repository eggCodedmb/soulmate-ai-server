package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.Notification;
import com.soulmate.domain.enums.NotificationType;
import com.soulmate.mapper.CompanionMapper;
import com.soulmate.mapper.NotificationMapper;
import com.soulmate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final CompanionMapper companionMapper;

    @Override
    public List<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getDeleted, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, userId)
                        .set(Notification::getReadStatus, 1));
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getReadStatus, 0)
                        .set(Notification::getReadStatus, 1));
    }

    @Override
    public void sendProactiveCare(Long userId, Long companionId) {
        Companion companion = companionMapper.selectById(companionId);
        if (companion == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setCompanionId(companionId);
        notification.setType(NotificationType.PROACTIVE_CARE);
        notification.setTitle(companion.getName() + " 想你了");
        notification.setContent(generateProactiveCareContent(companion));
        notification.setReadStatus(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        log.info("主动关心通知已发送: userId={}, companionId={}", userId, companionId);
    }

    private record TimeGreeting(int endHour, String template) {}

    private static final List<TimeGreeting> GREETINGS = List.of(
            new TimeGreeting(9,  "%s 给你发来了早安问候：新的一天开始了，记得吃早餐哦～"),
            new TimeGreeting(12, "%s 提醒你：上午工作辛苦了，适当休息一下吧～"),
            new TimeGreeting(14, "%s 关心你：中午记得好好吃饭，下午才有精神！"),
            new TimeGreeting(18, "%s 想和你聊聊天，今天过得怎么样？"),
            new TimeGreeting(22, "%s 在等你：忙了一天了，来聊聊天放松一下吧～"),
            new TimeGreeting(24, "%s 给你道晚安：今天辛苦了，早点休息哦～")
    );

    /**
     * 生成主动关心内容
     */
    private String generateProactiveCareContent(Companion companion) {
        int hour = LocalDateTime.now().getHour();
        String name = companion.getName();
        for (TimeGreeting greeting : GREETINGS) {
            if (hour < greeting.endHour()) {
                return String.format(greeting.template(), name);
            }
        }
        return String.format(GREETINGS.getLast().template(), name);
    }
}
