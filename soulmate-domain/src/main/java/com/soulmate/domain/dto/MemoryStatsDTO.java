package com.soulmate.domain.dto;

import lombok.Data;

/**
 * 长期记忆统计信息 DTO
 */
@Data
public class MemoryStatsDTO {
    private Integer totalMemories;
    private Double averageImportance;
    private Integer categoryCount;
}
