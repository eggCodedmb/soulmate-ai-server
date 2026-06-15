package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.dto.MemoryDTO;
import com.soulmate.domain.entity.Memory;
import com.soulmate.domain.enums.MemoryCategory;
import com.soulmate.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆管理接口
 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * 获取记忆列表 (支持过滤)
     */
    @GetMapping("/list")
    public R<List<MemoryDTO>> getMemories(@RequestAttribute("currentUserId") Long userId,
                                           @RequestParam(required = false) Long companionId,
                                           @RequestParam(required = false) MemoryCategory category) {
        return R.ok(memoryService.listMemories(userId, companionId, category));
    }

    /**
     * 手动创建记忆
     */
    @PostMapping
    public R<Void> createMemory(@RequestAttribute("currentUserId") Long userId,
                                 @RequestBody Memory memory) {
        memoryService.saveMemory(userId, memory);
        return R.ok();
    }

    /**
     * 更新记忆
     */
    @PutMapping("/{id}")
    public R<Void> updateMemory(@RequestAttribute("currentUserId") Long userId,
                                 @PathVariable Long id,
                                 @RequestBody Memory memory) {
        memoryService.updateMemory(userId, id, memory);
        return R.ok();
    }

    /**
     * 删除记忆
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteMemory(@RequestAttribute("currentUserId") Long userId,
                                 @PathVariable Long id) {
        memoryService.deleteMemory(userId, id);
        return R.ok();
    }

    /**
     * 重建向量数据库并将所有 MySQL 中的历史记忆重新同步到向量数据库中
     */
    @PostMapping("/admin/rebuild-vectors")
    public R<String> rebuildVectors() {
        memoryService.rebuildMemoryVectors();
        return R.ok("向量数据库集合重建及历史数据重同步成功");
    }
}
