package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置表
 */
@Data
@TableName("t_system_config")
public class SystemConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值（JSON字符串） */
    private String configValue;

    /** 配置说明 */
    private String description;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
