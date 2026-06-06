package com.soulmate.web.config;

import com.soulmate.common.config.FileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    /**
     * 配置静态资源映射，将文件访问URL映射到本地存储目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + fileProperties.getBaseDir() + "/";
        registry.addResourceHandler(fileProperties.getUrlPrefix() + "/**")
                .addResourceLocations(location);
    }
}
