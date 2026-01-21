package com.uni.research.module.auth.service;

import com.uni.research.module.auth.dto.LoginRequest;
import com.uni.research.module.auth.dto.LoginResponse;
import com.uni.research.module.auth.dto.RegisterRequest;

/**
 * 认证服务接口
 * 
 * @author wrench1024
 * @since 2026-01-03
 */
public interface AuthService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     * 
     * @param token    当前 Token
     * @param username 用户名
     */
    void logout(String token, String username);
}
