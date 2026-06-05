package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息表
 */
@Data
@TableName("t_notification")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联伴侣ID */
    private Long companionId;

    /** 通知类型 */
    private NotificationType type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 已读：0-未读 1-已读 */
    private Integer readStatus;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
