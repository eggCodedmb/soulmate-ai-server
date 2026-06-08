package com.soulmate.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑定时提醒请求
 */
@Data
public class UpdateReminderRequest {

    private Long companionId;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时间格式必须为 HH:mm")
    private String reminderTime;

    /** 重复星期，如 "1,2,3,4,5" (1=周一, 7=周日)，空代表仅一次 */
    private String repeatDays;

    @Size(max = 512, message = "说话模板不能超过512个字符")
    private String textTemplate;

    @Pattern(regexp = "^(WAKE_UP|NOTIFICATION)$", message = "类型必须为 WAKE_UP 或 NOTIFICATION")
    private String type;

    private Integer enabled;
}
