package com.soulmate.domain.dto;

import com.soulmate.domain.enums.MemoryCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆详情 DTO
 */
@Data
public class MemoryDTO {
    private Long id;
    private Long userId;
    private Long companionId;
    private MemoryCategory category;
    private String categoryDesc;
    private String title;
    private String content;
    private String thought; // AI的内心独白
    private String emotion; // 情绪标签 (用于显示图标)
    private Integer importance;
    private Integer userEdited;
    private LocalDateTime createTime;
    private String timeDesc; // 格式化后的时间，如 "2026年6月7日"
}
