package com.soulmate.web.config;

import com.soulmate.web.controller.CallWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * AI 通话 WebSocket 配置类
 * 注册 Raw WebSocket 端点 /ws/call
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CallWebSocketConfig implements WebSocketConfigurer {

    private final CallWebSocketHandler callWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callWebSocketHandler, "/ws/call")
                .setAllowedOrigins("*");
    }
}
