package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.dto.CompanionReminderVo;
import com.soulmate.domain.entity.CompanionReminder;
import com.soulmate.service.CompanionReminderService;
import com.soulmate.web.dto.CreateReminderRequest;
import com.soulmate.web.dto.UpdateReminderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI伴侣定时提醒控制器
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class CompanionReminderController {

    private final CompanionReminderService companionReminderService;

    /**
     * 获取当前用户的全部定时提醒列表
     */
    @GetMapping("/list")
    public R<List<CompanionReminderVo>> getReminderList(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(companionReminderService.getUserReminders(userId));
    }

    /**
     * 获取定时提醒详情
     */
    @GetMapping("/{id}")
    public R<CompanionReminderVo> getReminderDetail(@RequestAttribute("currentUserId") Long userId,
                                                     @PathVariable Long id) {
        return R.ok(companionReminderService.getReminderDetail(userId, id));
    }

    /**
     * 创建定时提醒
     */
    @PostMapping
    public R<CompanionReminder> createReminder(@RequestAttribute("currentUserId") Long userId,
                                                @Valid @RequestBody CreateReminderRequest request) {
        CompanionReminder reminder = new CompanionReminder();
        reminder.setCompanionId(request.getCompanionId());
        reminder.setReminderTime(request.getReminderTime());
        reminder.setRepeatDays(request.getRepeatDays() != null ? request.getRepeatDays() : "");
        reminder.setTextTemplate(request.getTextTemplate());
        reminder.setType(request.getType());
        reminder.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);

        return R.ok(companionReminderService.createReminder(userId, reminder));
    }

    /**
     * 更新定时提醒
     */
    @PutMapping("/{id}")
    public R<Void> updateReminder(@RequestAttribute("currentUserId") Long userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateReminderRequest request) {
        CompanionReminder reminder = new CompanionReminder();
        reminder.setCompanionId(request.getCompanionId());
        reminder.setReminderTime(request.getReminderTime());
        reminder.setRepeatDays(request.getRepeatDays());
        reminder.setTextTemplate(request.getTextTemplate());
        reminder.setType(request.getType());
        reminder.setEnabled(request.getEnabled());

        companionReminderService.updateReminder(userId, id, reminder);
        return R.ok();
    }

    /**
     * 删除定时提醒
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteReminder(@RequestAttribute("currentUserId") Long userId,
                                   @PathVariable Long id) {
        companionReminderService.deleteReminder(userId, id);
        return R.ok();
    }
}
