package com.soulmate.web.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private boolean isNewUser;
}
