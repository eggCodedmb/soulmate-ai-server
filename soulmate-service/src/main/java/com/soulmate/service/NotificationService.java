package com.soulmate.service;

import com.soulmate.domain.entity.Notification;

import java.util.List;

/**
 * 通知服务
 */
public interface NotificationService {

    /**
     * 获取用户通知列表
     */
    List<Notification> getUserNotifications(Long userId, int page, int size);

    /**
     * 标记通知已读
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);

    /**
     * 发送主动关心通知
     */
    void sendProactiveCare(Long userId, Long companionId);
}
