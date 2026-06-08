package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI伴侣定时叫醒与通知提醒实体类
 */
@Data
@TableName("t_companion_reminder")
public class CompanionReminder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联的伴侣ID */
    private Long companionId;

    /** 提醒时间，格式 HH:mm，例如 "07:30" */
    private String reminderTime;

    /** 重复星期，逗号分隔，例如 "1,2,3,4,5" (1=周一, 7=周日)，空代表仅一次 */
    private String repeatDays;

    /** 主动说话叫醒的文本模板 */
    private String textTemplate;

    /** 提醒类型：WAKE_UP (叫醒) 或 NOTIFICATION (通知) */
    private String type;

    /** 是否启用：1=启用，0=停用 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
