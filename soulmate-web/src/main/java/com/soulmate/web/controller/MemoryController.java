package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.entity.*;
import com.soulmate.domain.enums.MemoryCategory;
import com.soulmate.service.MemoryService;
import com.soulmate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆控制器
 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * 获取记忆列表
     */
    @GetMapping("/list")
    public R<List<Memory>> getMemories(@RequestAttribute("currentUserId") Long userId,
                                        @RequestParam(required = false) Long companionId,
                                        @RequestParam(required = false) MemoryCategory category) {
        return R.ok(memoryService.getUserMemories(userId, companionId, category));
    }

    /**
     * 编辑记忆
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
}
