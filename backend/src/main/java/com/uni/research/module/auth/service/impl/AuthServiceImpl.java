package com.uni.research.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uni.research.common.exception.BizException;
import com.uni.research.common.result.ResultCode;
import com.uni.research.common.service.TokenCacheService;
import com.uni.research.common.util.JwtUtils;
import com.uni.research.module.auth.dto.LoginRequest;
import com.uni.research.module.auth.dto.LoginResponse;
import com.uni.research.module.auth.dto.RegisterRequest;
import com.uni.research.module.auth.entity.User;
import com.uni.research.module.auth.mapper.UserMapper;
import com.uni.research.module.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 * 
 * @author wrench1024
 * @since 2026-01-03
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenCacheService tokenCacheService;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // 毫秒

    @Override
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        // 2. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 盐值加密
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1); // 默认正常

        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 获取用户信息
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.WRONG_PASSWORD, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 2. 生成 JWT Token
        // 注意：JWT 本身包含用户信息和过期时间，是无状态的
        String token = jwtUtils.generateToken(user.getUsername());

        // 3. 缓存 Token 到 Redis
        // 目的：
        // a. 支持登出功能（黑名单机制需要知道 Token 是否未过期）
        // b. 支持管理端查询在线用户列表
        // c. 实现单点登录互斥（可选）
        tokenCacheService.cacheToken(user.getUsername(), token, jwtExpiration / 1000);

        // 4. 返回结果
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void logout(String token, String username) {
        // 1. 将 Token 加入黑名单（剩余有效期内都不可用）
        // 核心安全逻辑：防止 JWT 被盗用后在服务端无法注销
        tokenCacheService.addToBlacklist(token, jwtExpiration / 1000);

        // 2. 删除用户缓存的 Token
        // 业务逻辑：标记用户为离线状态
        tokenCacheService.removeToken(username);
    }
}
