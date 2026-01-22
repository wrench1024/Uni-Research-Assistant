package com.uni.research.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 
 * 对应 408 考点：计算机网络 - 跨域资源共享（CORS）
 * 
 * 设计思路：
 * 1. 配置 CORS 允许前端跨域访问
 * 2. 开发环境允许所有域名，生产环境需限制
 * 3. 提供 RestTemplate Bean 用于调用外部服务
 * 4. 启用异步支持用于后台任务
 * 
 * @author wrench1024
 * @since 2026-01-02
 */
@Configuration
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*") // 允许所有域名（生产环境需限制）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 预检请求缓存时间（秒）
    }

    /**
     * RestTemplate Bean
     * 用于调用 Python AI 服务等外部 API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
