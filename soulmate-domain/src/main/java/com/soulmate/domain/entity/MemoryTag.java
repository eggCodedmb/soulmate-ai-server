package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆标签表
 */
@Data
@TableName("t_memory_tag")
public class MemoryTag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 记忆ID */
    private Long memoryId;

    /** 标签名（如"生日"、"旅行"、"宠物"） */
    private String tagName;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
