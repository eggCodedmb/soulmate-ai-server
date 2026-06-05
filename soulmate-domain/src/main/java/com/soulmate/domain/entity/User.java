package com.soulmate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.soulmate.domain.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 邮箱（登录凭证） */
    private String email;

    /** 密码哈希（验证码登录可为空） */
    private String passwordHash;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像URL */
    private String avatarUrl;

    /** 性别 */
    private Gender gender;

    /** 生日 */
    private LocalDate birthday;

    /** 是否游客：0-否 1-是 */
    private Integer guestFlag;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
