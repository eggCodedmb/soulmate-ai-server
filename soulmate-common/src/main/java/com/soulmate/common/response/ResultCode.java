package com.soulmate.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),
    FAIL(500, "系统错误"),

    // 认证相关 1xxx
    UNAUTHORIZED(1001, "未登录或Token已过期"),
    FORBIDDEN(1002, "无权限访问"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),
    VERIFY_CODE_ERROR(1006, "验证码错误或已过期"),
    VERIFY_CODE_FREQUENT(1007, "验证码发送过于频繁，请稍后再试"),

    // 参数相关 2xxx
    PARAM_ERROR(2001, "参数错误"),
    PARAM_MISSING(2002, "缺少必要参数"),
    PARAM_TYPE_ERROR(2003, "参数类型错误"),

    // 用户相关 3xxx
    USER_NOT_FOUND(3001, "用户不存在"),
    EMAIL_ALREADY_EXISTS(3002, "邮箱已被注册"),
    NICKNAME_INVALID(3003, "昵称不合法"),

    // 伴侣相关 4xxx
    COMPANION_NOT_FOUND(4001, "伴侣不存在"),
    COMPANION_LIMIT_REACHED(4002, "已达伴侣数量上限"),
    COMPANION_NAME_INVALID(4003, "伴侣名字不合法"),

    // 对话相关 5xxx
    CONVERSATION_NOT_FOUND(5001, "会话不存在"),
    DAILY_MESSAGE_LIMIT(5002, "今日消息已达上限，请升级会员"),

    // 订阅相关 6xxx
    PLAN_NOT_FOUND(6001, "套餐不存在"),
    SUBSCRIPTION_EXPIRED(6002, "订阅已过期"),
    PAYMENT_FAILED(6003, "支付失败"),

    // AI 相关 7xxx
    AI_SERVICE_ERROR(7001, "AI服务暂时不可用，请稍后再试"),
    AI_RESPONSE_TIMEOUT(7002, "AI响应超时"),

    // 文件相关 8xxx
    FILE_TYPE_NOT_ALLOWED(8001, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(8002, "文件大小超出限制"),
    FILE_UPLOAD_FAILED(8003, "文件上传失败"),
    FILE_NOT_FOUND(8004, "文件不存在"),
    FILE_DELETE_FAILED(8005, "文件删除失败");

    private final int code;
    private final String message;
}
