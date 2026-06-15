package com.soulmate.web.config;

import com.soulmate.web.controller.CallWebSocketHandler;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * AI 通话 WebSocket 配置类
 * 注册 Raw WebSocket 端点 /ws/call
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CallWebSocketConfig implements WebSocketConfigurer, ServletContextAware {

    private final CallWebSocketHandler callWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callWebSocketHandler, "/ws/call")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024 * 10); // 10MB
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 10); // 10MB
        return container;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (servletContext != null) {
            // 设置 Tomcat WebSocket 底层的发送/接收缓冲区大小为 10MB，防止大音频 Base64 触发 1009 (decoded text message was too big for the output buffer)
            servletContext.setAttribute("org.apache.tomcat.websocket.textBufferSize", 1024 * 1024 * 10);
            servletContext.setAttribute("org.apache.tomcat.websocket.binaryBufferSize", 1024 * 1024 * 10);
        }
    }
}


