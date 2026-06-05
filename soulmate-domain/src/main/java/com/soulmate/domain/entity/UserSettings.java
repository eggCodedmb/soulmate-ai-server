package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设置表
 */
@Data
@TableName("t_user_settings")
public class UserSettings {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 暗黑模式：0-跟随系统 1-开 2-关 */
    private Integer darkMode;

    /** 字体大小：small/normal/large */
    private String fontSize;

    /** 语言 */
    private String language;

    /** 消息通知：0-关 1-开 */
    private Integer messageNotify;

    /** 主动关心：0-关 1-开 */
    private Integer proactiveCare;

    /** 自定义模型地址（LM Studio/Ollama） */
    private String modelBaseUrl;

    /** 当前使用的模型名称 */
    private String modelName;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
