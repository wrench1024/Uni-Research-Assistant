package com.uni.research.module.chat.service;

import com.uni.research.module.chat.entity.ChatMessage;
import com.uni.research.module.chat.entity.ChatSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    /**
     * 创建新会话
     * 
     * @param userId 用户ID
     * @param title  会话标题
     * @return 会话实体
     */
    ChatSession createSession(Long userId, String title);

    /**
     * 获取会话历史消息
     * 
     * @param sessionId 会话ID
     * @param userId    用户ID (用于鉴权)
     * @return 消息列表
     */
    List<ChatMessage> getHistory(Long sessionId, Long userId);

    /**
     * 发送消息并获取 SSE 流
     * 
     * @param userId    用户ID
     * @param sessionId 会话ID (如果为 null 则自动创建)
     * @param content   用户消息内容
     * @return SSE Emitter
     */
    SseEmitter streamChat(Long userId, Long sessionId, String content);

    /**
     * 获取用户的所有会话列表
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getSessions(Long userId);

    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     * @param userId    用户ID (用于鉴权)
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 更新会话标题
     * 
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param title     新标题
     */
    void updateSessionTitle(Long sessionId, Long userId, String title);
}
