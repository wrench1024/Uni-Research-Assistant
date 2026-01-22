package com.uni.research.module.writing.controller;

import com.uni.research.module.writing.dto.WritingRequest;
import com.uni.research.module.writing.service.WritingService;
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
@RequestMapping("/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;
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

    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter process(@RequestBody WritingRequest request) {
        Long userId = getUserId();
        return writingService.processText(userId, request.getText(), request.getInstruction(), request.getContext());
    }
}
