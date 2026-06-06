package com.soulmate.service.impl;

import com.soulmate.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerifyCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("【SoulMate AI】登录验证码");
            helper.setText(buildVerifyCodeEmail(code), true);
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
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f5f5f5; font-family:'Microsoft YaHei','PingFang SC','Helvetica Neue',Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f5f5; padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.08); overflow:hidden;">
                                    <!-- 头部 -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding:32px 40px; text-align:center;">
                                            <h1 style="margin:0; color:#ffffff; font-size:24px; font-weight:600; letter-spacing:1px;">SoulMate AI</h1>
                                            <p style="margin:8px 0 0; color:rgba(255,255,255,0.85); font-size:13px;">你的灵魂伴侣，懂你所想</p>
                                        </td>
                                    </tr>
                                    <!-- 内容 -->
                                    <tr>
                                        <td style="padding:36px 40px;">
                                            <p style="margin:0 0 20px; font-size:15px; color:#333333; line-height:1.6;">
                                                你好！
                                            </p>
                                            <p style="margin:0 0 24px; font-size:15px; color:#333333; line-height:1.6;">
                                                你正在登录 <strong>SoulMate AI</strong>，请使用以下验证码完成验证：
                                            </p>
                                            <!-- 验证码区域 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="padding:20px 0;">
                                                        <div style="background-color:#f8f9ff; border:2px dashed #667eea; border-radius:8px; padding:20px 32px; display:inline-block;">
                                                            <span style="font-size:36px; font-weight:700; color:#667eea; letter-spacing:8px; font-family:'Courier New',monospace;">%s</span>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:24px 0 0; font-size:13px; color:#999999; line-height:1.6; text-align:center;">
                                                验证码 <strong style="color:#667eea;">5 分钟</strong>内有效，请勿泄露给他人
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 底部 -->
                                    <tr>
                                        <td style="padding:20px 40px; background-color:#fafafa; border-top:1px solid #f0f0f0;">
                                            <p style="margin:0; font-size:12px; color:#bbbbbb; line-height:1.6; text-align:center;">
                                                如非本人操作，请忽略此邮件，您的账号安全不会受到影响<br>
                                                © SoulMate AI Team
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(code);
    }
}
