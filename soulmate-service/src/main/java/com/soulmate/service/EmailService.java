package com.soulmate.service;

/**
 * 邮件服务
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     *
     * @param toEmail 收件邮箱
     * @param code    验证码
     */
    void sendVerifyCode(String toEmail, String code);
}
