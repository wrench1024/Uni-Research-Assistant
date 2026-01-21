package com.uni.research.common.filter;

import com.uni.research.common.service.TokenCacheService;
import com.uni.research.common.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 
 * 对应 408 考点：计算机网络 - 应用层协议（HTTP）与安全性
 * 
 * 设计思路：
 * 1. 继承 OncePerRequestFilter，确保每个请求只过滤一次
 * 2. 从请求头中提取 Authorization: Bearer <token>
 * 3. 验证 Token 有效性并提取用户信息
 * 4. 检查 Token 是否在黑名单中（登出的 Token）
 * 5. 将用户信息存入 SecurityContextHolder 供后续权限校验使用
 * 
 * 面试话术：
 * "我实现了一个自定义的 JWT 过滤切面。
 * 它的工作原理是拦截每个受限请求，解析 HTTP Header 中的 Bearer Token。
 * 过滤器是基于 Servlet 规范实现的，处于 Spring Security 过滤器链的上游。
 * 这种设计体现了拦截器模式，能够实现对敏感接口的统一安全管控。
 * 同时我引入了 Redis 黑名单机制，使得用户登出后 Token 立即失效。"
 * 
 * @author wrench1024
 * @since 2026-01-03
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final TokenCacheService tokenCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. 判断是否有 JWT Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取并解析 Token
        jwt = authHeader.substring(7);
        try {
            username = jwtUtils.extractUsername(jwt);

            // 3. 检查 Token 是否在黑名单中（登出的 Token）
            // 面试考点：为什么要在验签之前/之后做？
            // 这里放在提取 username 之后、验签之前。
            // 只要 Token 格式正确且能解析出 username，就先查黑名单。
            // 如果在黑名单中，直接拦截，不再进行后续的高成本操作（如查库加载 UserDetails）。
            if (tokenCacheService.isBlacklisted(jwt)) {
                // 如果 Token 已拉黑，中断过滤链，不调用 filterChain.doFilter
                // 此时 SecurityContext 为空，Spring Security 后续的 ExceptionTranslationFilter 会抛出 403
                // Forbidden
                return;
            }

            // 4. 如果 Token 有效且上下文未认证，则存入上下文
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtils.validateToken(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // 解析失败（过期或伪造），这里不做处理，交给后续的 EntryPoint 抛出异常或直接放行让 Security 拦截
        }

        filterChain.doFilter(request, response);
    }
}
