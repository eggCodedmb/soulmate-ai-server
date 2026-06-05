package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.entity.User;
import com.soulmate.domain.entity.UserProfile;
import com.soulmate.domain.entity.UserSettings;
import com.soulmate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public R<User> getUserInfo(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(userService.getUserById(userId));
    }

    /**
     * 获取用户资料
     */
    @GetMapping("/profile")
    public R<UserProfile> getProfile(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(userService.getUserProfile(userId));
    }

    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestAttribute("currentUserId") Long userId,
                                  @RequestBody UserProfile profile) {
        userService.updateUserProfile(userId, profile);
        return R.ok();
    }

    /**
     * 获取用户设置
     */
    @GetMapping("/settings")
    public R<UserSettings> getSettings(@RequestAttribute("currentUserId") Long userId) {
        return R.ok(userService.getUserSettings(userId));
    }

    /**
     * 更新用户设置
     */
    @PutMapping("/settings")
    public R<Void> updateSettings(@RequestAttribute("currentUserId") Long userId,
                                   @RequestBody UserSettings settings) {
        userService.updateUserSettings(userId, settings);
        return R.ok();
    }
}
