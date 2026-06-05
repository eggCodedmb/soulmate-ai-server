package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.RepeatType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日程提醒表
 */
@Data
@TableName("t_schedule_reminder")
public class ScheduleReminder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联伴侣ID */
    private Long companionId;

    /** 提醒标题 */
    private String title;

    /** 提醒内容 */
    private String content;

    /** 提醒时间 */
    private LocalDateTime remindTime;

    /** 重复类型 */
    private RepeatType repeatType;

    /** 状态：0-待触发 1-已触发 2-已取消 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
