package com.soulmate.service.impl;

import com.soulmate.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private String verifyCodeTemplate;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("email.html");
            this.verifyCodeTemplate = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("邮件模板加载成功");
        } catch (IOException e) {
            log.error("加载邮件模板失败", e);
            throw new RuntimeException("无法加载邮件模板 email.html", e);
        }
    }

    @Override
    public void sendVerifyCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("【SoulMate AI】登录验证码");
            helper.setText(verifyCodeTemplate.formatted(code), true);
            mailSender.send(message);
            log.info("验证码邮件已发送: email={}", toEmail);
        } catch (Exception e) {
            log.warn("邮件发送失败，降级为日志输出: email={}, code={}, error={}",
                    toEmail, code, e.getMessage());
            log.info("【开发环境】验证码: email={}, code={}", toEmail, code);
        }
    }
}
