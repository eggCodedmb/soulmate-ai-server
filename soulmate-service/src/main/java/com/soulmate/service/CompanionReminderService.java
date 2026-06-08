package com.soulmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.soulmate.domain.entity.CompanionReminder;
import com.soulmate.domain.dto.CompanionReminderVo;

import java.util.List;

/**
 * AI伴侣定时提醒服务接口
 */
public interface CompanionReminderService extends IService<CompanionReminder> {

    /**
     * 获取用户的全部提醒列表（包含伴侣信息）
     */
    List<CompanionReminderVo> getUserReminders(Long userId);

    /**
     * 获取提醒详情
     */
    CompanionReminderVo getReminderDetail(Long userId, Long reminderId);

    /**
     * 创建定时提醒
     */
    CompanionReminder createReminder(Long userId, CompanionReminder reminder);

    /**
     * 修改定时提醒
     */
    void updateReminder(Long userId, Long reminderId, CompanionReminder reminder);

    /**
     * 删除定时提醒
     */
    void deleteReminder(Long userId, Long reminderId);

    /**
     * 从 AI 对话内容中解析并自动创建提醒
     */
    void parseAndCreateReminder(Long userId, Long companionId, String content);
}
