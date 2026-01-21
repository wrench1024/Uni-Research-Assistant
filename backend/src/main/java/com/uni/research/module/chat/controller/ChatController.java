package com.uni.research.module.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uni.research.common.result.Result;
import com.uni.research.module.auth.entity.User;
import com.uni.research.module.auth.mapper.UserMapper;
import com.uni.research.module.chat.dto.ChatSendRequest;
import com.uni.research.module.chat.entity.ChatMessage;
import com.uni.research.module.chat.entity.ChatSession;
import com.uni.research.module.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
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

    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestParam(defaultValue = "新会话") String title) {
        Long userId = getUserId();
        return Result.success(chatService.createSession(userId, title));
    }

    /**
     * 获取当前用户的所有会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions() {
        Long userId = getUserId();
        return Result.success(chatService.getSessions(userId));
    }

    /**
     * 删除会话
     */
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = getUserId();
        chatService.deleteSession(sessionId, userId);
        return Result.success(null);
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/session/{sessionId}")
    public Result<Void> updateSessionTitle(@PathVariable Long sessionId, @RequestParam String title) {
        Long userId = getUserId();
        chatService.updateSessionTitle(sessionId, userId, title);
        return Result.success(null);
    }

    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessage>> getHistory(@PathVariable Long sessionId) {
        Long userId = getUserId();
        return Result.success(chatService.getHistory(sessionId, userId));
    }

    /**
     * 发送消息并开启 SSE 流
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE 确保返回 Content-Type:
     * text/event-stream
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@RequestBody ChatSendRequest sendRequest) {
        Long userId = getUserId();
        return chatService.streamChat(userId, sendRequest.getSessionId(), sendRequest.getContent());
    }
}
