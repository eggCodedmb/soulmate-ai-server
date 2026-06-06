package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.soulmate.domain.enums.RelationshipType;
import com.soulmate.domain.enums.SpeakingStyle;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI伴侣表
 */
@Data
@TableName("t_companion")
public class Companion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 伴侣名字 */
    private String name;

    /** 性别：1-男 2-女 3-非二元 */
    private Integer gender;

    /** 关系类型 */
    private RelationshipType relationshipType;

    /** 背景故事/描述 */
    private String description;

    /** 说话风格 */
    private SpeakingStyle speakingStyle;

    /** 当前头像URL */
    private String avatarUrl;

    /** 主题色（由性格决定） */
    private String themeColor;

    /** 状态：0-归档 1-活跃 */
    private Integer status;

    /** 排序权重（置顶等） */
    private Integer companionOrder;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 性格标签（非数据库字段，由 service 层填充） */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> personalityKeys;
}
