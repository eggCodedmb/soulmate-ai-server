package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.entity.User;
import com.soulmate.common.config.JwtProperties;
import com.soulmate.common.util.JwtUtil;
import com.soulmate.service.UserService;
import com.soulmate.web.dto.LoginRequest;
import com.soulmate.web.dto.LoginResponse;
import com.soulmate.web.dto.SendCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public R<Void> sendVerifyCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendVerifyCode(request.getEmail());
        return R.ok();
    }

    /**
     * 邮箱验证码登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.loginByEmail(request.getEmail(), request.getVerifyCode());
        String token = JwtUtil.generateToken(user.getId(), jwtProperties.getSecret(), jwtProperties.getExpireMs());
        return R.ok(LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(user.getLastLoginTime() == null)
                .build());
    }

    /**
     * 游客登录
     */
    @PostMapping("/guest")
    public R<LoginResponse> guestLogin() {
        User user = userService.loginAsGuest();
        String token = JwtUtil.generateToken(user.getId(), jwtProperties.getSecret(), jwtProperties.getExpireMs());
        return R.ok(LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .isNewUser(true)
                .build());
    }
}
