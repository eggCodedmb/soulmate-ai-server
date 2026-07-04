package com.soulmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.dto.CompanionReminderVo;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.CompanionReminder;
import com.soulmate.mapper.CompanionReminderMapper;
import com.soulmate.service.CompanionReminderService;
import com.soulmate.service.CompanionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI伴侣定时提醒服务接口实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionReminderServiceImpl extends ServiceImpl<CompanionReminderMapper, CompanionReminder>
        implements CompanionReminderService {

    private final CompanionService companionService;

    @Override
    public List<CompanionReminderVo> getUserReminders(Long userId) {
        List<CompanionReminder> list = list(new LambdaQueryWrapper<CompanionReminder>()
                .eq(CompanionReminder::getUserId, userId)
                .orderByAsc(CompanionReminder::getReminderTime));
        return list.stream().map(r -> convertToVo(userId, r)).collect(Collectors.toList());
    }

    @Override
    public CompanionReminderVo getReminderDetail(Long userId, Long reminderId) {
        CompanionReminder reminder = getById(reminderId);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new BizException(ResultCode.REMINDER_NOT_FOUND);
        }
        return convertToVo(userId, reminder);
    }

    @Override
    @Transactional
    public CompanionReminder createReminder(Long userId, CompanionReminder reminder) {
        // 校验伴侣是否存在且属于当前用户
        companionService.getCompanionDetail(userId, reminder.getCompanionId());

        reminder.setUserId(userId);
        if (reminder.getEnabled() == null) {
            reminder.setEnabled(1);
        }
        reminder.setCreateTime(LocalDateTime.now());
        reminder.setUpdateTime(LocalDateTime.now());
        save(reminder);
        return reminder;
    }

    @Override
    @Transactional
    public void updateReminder(Long userId, Long reminderId, CompanionReminder reminder) {
        CompanionReminder existing = getById(reminderId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException(ResultCode.REMINDER_NOT_FOUND);
        }

        // 如果修改了关联伴侣，验证新伴侣
        if (reminder.getCompanionId() != null) {
            companionService.getCompanionDetail(userId, reminder.getCompanionId());
            existing.setCompanionId(reminder.getCompanionId());
        }

        if (reminder.getReminderTime() != null) {
            existing.setReminderTime(reminder.getReminderTime());
        }
        if (reminder.getRepeatDays() != null) {
            existing.setRepeatDays(reminder.getRepeatDays());
        }
        if (reminder.getTextTemplate() != null) {
            existing.setTextTemplate(reminder.getTextTemplate());
        }
        if (reminder.getType() != null) {
            existing.setType(reminder.getType());
        }
        if (reminder.getEnabled() != null) {
            existing.setEnabled(reminder.getEnabled());
        }

        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional
    public void deleteReminder(Long userId, Long reminderId) {
        CompanionReminder existing = getById(reminderId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException(ResultCode.REMINDER_NOT_FOUND);
        }
        removeById(reminderId);
    }

    @Override
    @Transactional
    public void parseAndCreateReminder(Long userId, Long companionId, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        // Pattern: <command type="create_reminder" time="([^"]+)" type_val="([^"]+)"(?:\s+repeat="([^"]*)")?>(.*?)</command>
        Pattern pattern = Pattern.compile("<command\\s+type=\"create_reminder\"\\s+time=\"([^\"]+)\"\\s+type_val=\"([^\"]+)\"(?:\\s+repeat=\"([^\"]*)\")?>(.*?)</command>");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            try {
                String time = matcher.group(1);
                String typeVal = matcher.group(2);
                String repeat = matcher.group(3);
                String template = matcher.group(4);

                CompanionReminder reminder = new CompanionReminder();
                reminder.setUserId(userId);
                reminder.setCompanionId(companionId);
                reminder.setReminderTime(time);
                reminder.setRepeatDays(repeat != null ? repeat : "");
                reminder.setTextTemplate(template);
                reminder.setType(typeVal);
                reminder.setEnabled(1);
                reminder.setCreateTime(LocalDateTime.now());
                reminder.setUpdateTime(LocalDateTime.now());

                save(reminder);
                log.info("AI 自动创建定时提醒成功, userId: {}, companionId: {}, 时间: {}", userId, companionId, time);
            } catch (Exception e) {
                log.error("AI 自动创建定时提醒解析失败, 内容: {}", content, e);
            }
        }
    }

    private CompanionReminderVo convertToVo(Long userId, CompanionReminder reminder) {
        CompanionReminderVo vo = new CompanionReminderVo();
        vo.setId(reminder.getId());
        vo.setUserId(reminder.getUserId());
        vo.setCompanionId(reminder.getCompanionId());
        vo.setReminderTime(reminder.getReminderTime());
        vo.setRepeatDays(reminder.getRepeatDays());
        vo.setTextTemplate(reminder.getTextTemplate());
        vo.setType(reminder.getType());
        vo.setEnabled(reminder.getEnabled());
        vo.setCreateTime(reminder.getCreateTime());
        vo.setUpdateTime(reminder.getUpdateTime());

        try {
            Companion companion = companionService.getCompanionDetail(userId, reminder.getCompanionId());
            if (companion != null) {
                vo.setCompanionName(companion.getName());
                vo.setCompanionAvatarUrl(companion.getAvatarUrl());
            }
        } catch (Exception e) {
            log.warn("填充提醒伴侣信息失败, companionId: {}", reminder.getCompanionId(), e);
        }
        return vo;
    }
}
