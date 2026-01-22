package com.uni.research.module.analysis.controller;

import com.uni.research.module.analysis.dto.AnalysisRequest;
import com.uni.research.module.analysis.dto.ComparisonRequest;
import com.uni.research.module.analysis.service.AnalysisService;
import com.uni.research.module.auth.mapper.UserMapper;
import com.uni.research.module.auth.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserMapper userMapper;

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user.getId();
    }

    @PostMapping(value = "/summary", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter summary(@RequestBody AnalysisRequest request) {
        Long userId = getUserId();
        return analysisService.analyzeSummary(userId, request.getDocId(), request.getType());
    }

    @PostMapping(value = "/comparison", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter comparison(@RequestBody ComparisonRequest request) {
        Long userId = getUserId();
        return analysisService.analyzeComparison(userId, request.getDocIds(), request.getAspects());
    }
}
