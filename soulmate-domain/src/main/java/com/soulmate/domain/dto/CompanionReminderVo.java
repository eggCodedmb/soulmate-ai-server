package com.soulmate.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI伴侣定时提醒 VO
 */
@Data
public class CompanionReminderVo {
    private Long id;
    private Long userId;
    private Long companionId;
    private String companionName;
    private String companionAvatarUrl;
    private String reminderTime;
    private String repeatDays;
    private String textTemplate;
    private String type;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
