package com.uni.research.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 
 * 对应 408 考点：数据结构 - 哈希表、操作系统 - 缓存策略
 * 
 * 面试话术：
 * "我使用 Redis 来缓存用户 Token，主要是考虑到两点：
 * 1. 实现登出功能：JWT 一旦签发服务器无法主动使其失效，但通过 Redis 黑名单可以拦截已登出的 Token；
 * 2. 单点登录控制：可以限制同一账号同时在线的设备数量。
 * 这体现了对分布式系统状态管理的理解。"
 * 
 * @author wrench1024
 * @since 2026-01-11
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * <p>
     * <b>面试考点：Redis 序列化方式的选择</b>
     * <ul>
     * <li><b>默认情况</b>：Spring Boot 默认使用 {@code JdkSerializationRedisSerializer}。
     * <br>
     * 缺点：序列化后的 value 是二进制流（类似 {@code \xac\xed\x00\x05}），体积大且在 Redis
     * 客户端中无法直接阅读调试。</li>
     * <li><b>改进方案</b>：我们这里显式配置了 JSON 序列化。
     * <br>
     * 优点：
     * <ol>
     * <li><b>可读性</b>：数据以 JSON 字符串存储，运维排查方便。</li>
     * <li><b>跨语言</b>：JSON 是通用标准，方便其他语言的服务（如 Python AI 服务）读取共享数据。</li>
     * </ol>
     * </li>
     * </ul>
     * 
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. Key 序列化策略：使用 StringRedisSerializer
        // 理由：Key 通常是简单的字符串（如 "token:user:xxx"），不需要复杂序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 2. Value 序列化策略：使用 GenericJackson2JsonRedisSerializer
        // 理由：Value 可能是对象（如 UserDTO），转为 JSON 存取最灵活
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
