package com.limiti.employee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * @author Limiti
 * @date 2024/11/15
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
} 