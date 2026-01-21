package com.uni.research.module.auth.controller;

import com.uni.research.common.result.Result;
import com.uni.research.module.auth.dto.LoginRequest;
import com.uni.research.module.auth.dto.LoginResponse;
import com.uni.research.module.auth.dto.RegisterRequest;
import com.uni.research.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 
 * @author wrench1024
 * @since 2026-01-03
 */
@Tag(name = "认证管理", description = "用户注册与登录")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // 1. 从 Standard HTTP Header 中提取 Token
        // 格式通常为 "Authorization: Bearer <token>"
        // 必须去掉 "Bearer " 前缀，否则存入 Redis 的 Key 会带有空格和多余字符，导致 Filter 拦截失效
        String token = authHeader.replace("Bearer ", "");

        // 2. 从 SecurityContext 获取当前认证用户
        // 此时用户认证通过，SecurityContext 中一定有值
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        // 3. 执行业务层登出逻辑
        authService.logout(token, username);
        return Result.success();
    }
}
