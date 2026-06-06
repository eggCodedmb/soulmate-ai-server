package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.MemoryCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 长期记忆表
 */
@Data
@TableName("t_memory")
public class Memory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联伴侣ID */
    private Long companionId;

    /** 分类 */
    private MemoryCategory category;

    /** 记忆标题 */
    private String title;

    /** 记忆内容 */
    private String content;

    /** AI的内心独白/感悟 */
    private String thought;

    /** 情绪标签 (用于显示图标) */
    private String emotion;

    /** 来源消息ID */
    private Long sourceMessageId;

    /** 重要度：1-10 */
    private Integer importance;

    /** Milvus中的向量ID */
    private String vectorId;

    /** 被检索引用次数 */
    private Integer accessCount;

    /** 最后被引用时间 */
    private LocalDateTime lastAccessTime;

    /** 是否对用户可见 */
    private Integer userVisible;

    /** 是否被用户编辑过 */
    private Integer userEdited;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
