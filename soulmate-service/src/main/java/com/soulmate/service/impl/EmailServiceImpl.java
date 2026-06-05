package com.soulmate.service.impl;

import com.soulmate.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerifyCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@soulmate-ai.com");
            message.setTo(toEmail);
            message.setSubject("SoulMate AI - 验证码");
            message.setText(buildVerifyCodeEmail(code));
            mailSender.send(message);
            log.info("验证码邮件已发送: email={}", toEmail);
        } catch (Exception e) {
            // 邮件发送失败时降级为日志输出（开发环境兼容）
            log.warn("邮件发送失败，降级为日志输出: email={}, code={}, error={}",
                    toEmail, code, e.getMessage());
            log.info("【开发环境】验证码: email={}, code={}", toEmail, code);
        }
    }

    private String buildVerifyCodeEmail(String code) {
        return """
                你好！

                你正在登录 SoulMate AI，验证码如下：

                %s

                验证码 5 分钟内有效，请勿泄露给他人。
                如非本人操作，请忽略此邮件。

                —— SoulMate AI 团队
                """.formatted(code);
    }
}
