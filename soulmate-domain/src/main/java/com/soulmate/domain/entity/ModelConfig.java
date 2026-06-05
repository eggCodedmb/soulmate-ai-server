package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型配置表
 */
@Data
@TableName("t_model_config")
public class ModelConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模型编码 */
    private String modelCode;

    /** 模型显示名 */
    private String modelName;

    /** 提供商：openai/anthropic/local/alibaba/xiaomi */
    private String provider;

    /** API基础地址 */
    private String baseUrl;

    /** API密钥（加密存储） */
    private String apiKey;

    /** 最大token数 */
    private Integer maxTokens;

    /** 温度参数 */
    private BigDecimal temperature;

    /** 支持流式输出：0-否 1-是 */
    private Integer supportStream;

    /** 支持图片理解：0-否 1-是 */
    private Integer supportVision;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
