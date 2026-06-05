package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.entity.Companion;
import com.soulmate.domain.entity.CompanionPersonality;
import com.soulmate.domain.enums.PersonalityKey;
import com.soulmate.service.CompanionService;
import com.soulmate.web.dto.CreateCompanionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 伴侣控制器
 */
@RestController
@RequestMapping("/api/companion")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;

    /**
     * 创建伴侣
     */
    @PostMapping
    public R<Companion> createCompanion(@RequestAttribute("currentUserId") Long userId,
                                         @Valid @RequestBody CreateCompanionRequest request) {
        Companion companion = new Companion();
        companion.setName(request.getName());
        companion.setGender(request.getGender());
        companion.setRelationshipType(
                com.soulmate.domain.enums.RelationshipType.valueOf(request.getRelationshipType().toUpperCase()));
        companion.setSpeakingStyle(
                com.soulmate.domain.enums.SpeakingStyle.valueOf(
                        request.getSpeakingStyle() != null ? request.getSpeakingStyle().toUpperCase() : "CASUAL"));
        companion.setDescription(request.getDescription());

        List<CompanionPersonality> personalities = null;
        if (request.getPersonalityKeys() != null) {
            personalities = request.getPersonalityKeys().stream()
                    .map(key -> {
                        CompanionPersonality p = new CompanionPersonality();
                        p.setPersonalityKey(PersonalityKey.valueOf(key.toUpperCase()));
                        return p;
                    })
                    .toList();
        }

        return R.ok(companionService.createCompanion(userId, companion, personalities));
    }

    /**
     * 获取伴侣列表
     */
    @GetMapping("/list")
    public R<List<Companion>> getCompanionList(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(companionService.getUserCompanions(userId));
    }

    /**
     * 获取伴侣详情
     */
    @GetMapping("/{id}")
    public R<Companion> getCompanion(@RequestAttribute("currentUserId") Long userId,
                                      @PathVariable Long id) {
        return R.ok(companionService.getCompanionDetail(userId, id));
    }

    /**
     * 编辑伴侣
     */
    @PutMapping("/{id}")
    public R<Void> updateCompanion(@RequestAttribute("currentUserId") Long userId,
                                    @PathVariable Long id,
                                    @RequestBody Companion companion) {
        companionService.updateCompanion(userId, id, companion);
        return R.ok();
    }

    /**
     * 删除伴侣
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteCompanion(@RequestAttribute("currentUserId") Long userId,
                                    @PathVariable Long id) {
        companionService.deleteCompanion(userId, id);
        return R.ok();
    }
}
