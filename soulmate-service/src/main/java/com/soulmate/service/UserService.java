package com.soulmate.service;

import com.soulmate.domain.entity.User;
import com.soulmate.domain.entity.UserProfile;
import com.soulmate.domain.entity.UserSettings;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 邮箱验证码登录（注册+登录一体）
     */
    User loginByEmail(String email, String verifyCode);

    /**
     * 游客登录
     */
    User loginAsGuest();

    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);

    /**
     * 获取用户资料
     */
    UserProfile getUserProfile(Long userId);

    /**
     * 更新用户资料
     */
    void updateUserProfile(Long userId, UserProfile profile);

    /**
     * 获取用户设置
     */
    UserSettings getUserSettings(Long userId);

    /**
     * 更新用户设置
     */
    void updateUserSettings(Long userId, UserSettings settings);

    /**
     * 发送邮箱验证码
     */
    void sendVerifyCode(String email);
}
