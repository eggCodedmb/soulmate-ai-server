package com.soulmate.web.config;

import com.soulmate.common.config.FileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executors;

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

    /**
     * 配置异步处理使用虚拟线程，替代默认的 SimpleAsyncTaskExecutor
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        AsyncTaskExecutor executor = new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        configurer.setTaskExecutor(executor);
    }
}
