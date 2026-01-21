package com.uni.research.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 缓存服务
 * 
 * 功能：
 * 1. 缓存用户 Token（登录时存入）
 * 2. Token 黑名单（登出时拉黑）
 * 3. 验证 Token 是否有效
 * 
 * 对应 408 考点：操作系统 - 缓存淘汰策略（TTL 过期）
 * 
 * @author wrench1024
 * @since 2026-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 前缀
    private static final String TOKEN_PREFIX = "token:user:";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 缓存用户 Token
     * <p>
     * <b>设计意图：</b>
     * 虽然 JWT 自包含用户信息，看似不需要服务端存储。但为了实现以下功能，我们将其存入 Redis：
     * <ol>
     * <li><b>在线状态管理</b>：可以通过查询 Redis 判断用户是否在线。</li>
     * <li><b>单点登录控制</b>：如果需要限制"一号一端"，可以在此处检查 key 是否已存在，若存在则踢出旧 Token。</li>
     * </ol>
     * 
     * @param username      用户名
     * @param token         JWT Token
     * @param expireSeconds 过期时间（秒），<b>必须与 JWT 的 exp claim 保持一致</b>，确保 Redis 数据与
     *                      Token 同时失效
     */
    public void cacheToken(String username, String token, long expireSeconds) {
        String key = TOKEN_PREFIX + username;
        redisTemplate.opsForValue().set(key, token, expireSeconds, TimeUnit.SECONDS);
        log.debug("Token cached for user: {}", username);
    }

    /**
     * 获取用户缓存的 Token
     */
    public String getCachedToken(String username) {
        String key = TOKEN_PREFIX + username;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除用户 Token（用于登出或强制下线）
     */
    public void removeToken(String username) {
        String key = TOKEN_PREFIX + username;
        redisTemplate.delete(key);
        log.debug("Token removed for user: {}", username);
    }

    /**
     * 将 Token 加入黑名单
     * <p>
     * <b>核心逻辑（面试高频点）：</b>
     * JWT 的最大痛点是 <b>"无法撤销"</b> (Stateless)。一旦签发，只要没过期且签名正确，服务器就必须认。
     * 为了实现"登出"或"强制下线"，我们采用 <b>Redis 黑名单机制</b>。
     * <p>
     * <b>TTL 设置策略：</b>
     * 黑名单 Key 的过期时间 = Token 的剩余有效期。
     * <br>
     * 例如：Token 还有 10 分钟过期，黑名单也只需存 10 分钟。
     * 10 分钟后，Token 自身过期（自然失效），黑名单 Key 也可以自动删除，<b>最大限度节省 Redis 内存</b>。
     * 
     * @param token         JWT Token 字符串
     * @param expireSeconds 黑名单保留时间（应等于 Token 剩余有效期）
     */
    public void addToBlacklist(String token, long expireSeconds) {
        String key = BLACKLIST_PREFIX + token;
        // Value 设为 1 即可，关键是 Key 的存在性
        redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
        log.debug("Token added to blacklist");
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
