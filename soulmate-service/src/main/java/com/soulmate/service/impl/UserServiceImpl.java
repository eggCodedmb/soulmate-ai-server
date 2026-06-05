package com.soulmate.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soulmate.common.config.JwtProperties;
import com.soulmate.common.constant.RedisConstants;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.common.util.JwtUtil;
import com.soulmate.domain.entity.User;
import com.soulmate.domain.entity.UserProfile;
import com.soulmate.domain.entity.UserSettings;
import com.soulmate.domain.enums.Gender;
import com.soulmate.mapper.UserMapper;
import com.soulmate.mapper.UserProfileMapper;
import com.soulmate.mapper.UserSettingsMapper;
import com.soulmate.service.EmailService;
import com.soulmate.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final EmailService emailService;

    @Override
    @Transactional
    public User loginByEmail(String email, String verifyCode) {
        // 校验验证码
        String cachedCode = redisTemplate.opsForValue().get(RedisConstants.VERIFY_CODE + email);
        if (cachedCode == null || !cachedCode.equals(verifyCode)) {
            throw new BizException(ResultCode.VERIFY_CODE_ERROR);
        }
        // 删除已使用的验证码
        redisTemplate.delete(RedisConstants.VERIFY_CODE + email);

        // 查找或创建用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setNickname("用户" + RandomUtil.randomNumbers(6));
            user.setGender(Gender.UNSET);
            user.setGuestFlag(0);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);

            // 创建默认设置
            createDefaultSettings(user.getId());
            // 创建默认资料
            createDefaultProfile(user.getId());
        }

        // 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    @Override
    @Transactional
    public User loginAsGuest() {
        User user = new User();
        user.setEmail("guest_" + RandomUtil.randomString(12) + "@soulmate.temp");
        user.setNickname("游客" + RandomUtil.randomNumbers(4));
        user.setGender(Gender.UNSET);
        user.setGuestFlag(1);
        user.setStatus(1);
        user.setLastLoginTime(LocalDateTime.now());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        createDefaultSettings(user.getId());
        createDefaultProfile(user.getId());

        return user;
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public UserProfile getUserProfile(Long userId) {
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    @Override
    public void updateUserProfile(Long userId, UserProfile profile) {
        UserProfile existing = getUserProfile(userId);
        if (existing != null) {
            profile.setId(existing.getId());
            profile.setUserId(userId);
            profile.setUpdateTime(LocalDateTime.now());
            userProfileMapper.updateById(profile);
        }
    }

    @Override
    public UserSettings getUserSettings(Long userId) {
        return userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId));
    }

    @Override
    public void updateUserSettings(Long userId, UserSettings settings) {
        UserSettings existing = getUserSettings(userId);
        if (existing != null) {
            settings.setId(existing.getId());
            settings.setUserId(userId);
            settings.setUpdateTime(LocalDateTime.now());
            userSettingsMapper.updateById(settings);
        }
    }

    @Override
    public void sendVerifyCode(String email) {
        // 检查发送频率
        String rateKey = RedisConstants.VERIFY_CODE_RATE + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new BizException(ResultCode.VERIFY_CODE_FREQUENT);
        }

        // 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        // 存入Redis
        redisTemplate.opsForValue().set(
                RedisConstants.VERIFY_CODE + email,
                code,
                RedisConstants.VERIFY_CODE_TTL_MINUTES,
                TimeUnit.MINUTES);

        // 设置发送频率限制
        redisTemplate.opsForValue().set(
                rateKey, "1",
                RedisConstants.VERIFY_CODE_RATE_SECONDS,
                TimeUnit.SECONDS);

        // 调用邮件服务发送验证码
        emailService.sendVerifyCode(email, code);
    }

    private void createDefaultSettings(Long userId) {
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        settings.setDarkMode(0);
        settings.setFontSize("normal");
        settings.setLanguage("zh-CN");
        settings.setMessageNotify(1);
        settings.setProactiveCare(1);
        settings.setCreateTime(LocalDateTime.now());
        settings.setUpdateTime(LocalDateTime.now());
        userSettingsMapper.insert(settings);
    }

    private void createDefaultProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(profile);
    }
}
