package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.SceneMode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话会话表
 */
@Data
@TableName("t_conversation")
public class Conversation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 伴侣ID */
    private Long companionId;

    /** 场景模式 */
    private SceneMode sceneMode;

    /** 最后一条消息预览 */
    private String lastMessagePreview;

    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 是否置顶 */
    private Integer pinned;

    /** 上下文窗口大小（轮数） */
    private Integer contextWindow;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
