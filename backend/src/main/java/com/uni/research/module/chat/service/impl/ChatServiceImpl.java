package com.uni.research.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.research.module.chat.entity.ChatMessage;
import com.uni.research.module.chat.entity.ChatSession;
import com.uni.research.module.chat.mapper.ChatMessageMapper;
import com.uni.research.module.chat.mapper.ChatSessionMapper;
import com.uni.research.module.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chat 服务实现类
 * 负责调用 Python AI 服务并将 SSE 流转发给前端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // OkHttp Client for calling Python Service
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(Duration.ofMinutes(5))
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    // Thread pool for async tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String PYTHON_SERVICE_URL = "http://localhost:8000/api/v1/chat/stream";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public List<ChatMessage> getHistory(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter streamChat(Long userId, Long sessionId, String content) {
        // 1. Get or Create Session
        Long finalSessionId;
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setUserId(userId);
            // Use first 20 chars as title
            String title = content.length() > 20 ? content.substring(0, 17) + "..." : content;
            session.setTitle(title);
            sessionMapper.insert(session);
            finalSessionId = session.getId();
        } else {
            finalSessionId = sessionId;
            // Check if it's the first message to update title if it's still default
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && "新会话".equals(session.getTitle())) {
                String title = content.length() > 20 ? content.substring(0, 17) + "..." : content;
                session.setTitle(title);
                sessionMapper.updateById(session);
            }
        }

        // Send sessionId as first event to frontend
        SseEmitter emitter = new SseEmitter(60000L);
        try {
            emitter.send(SseEmitter.event().data("{\"sessionId\":" + finalSessionId + "}"));
        } catch (Exception e) {
            return emitter;
        }

        executor.execute(() -> {
            try {
                // 2. Save User Message
                ChatMessage userMsg = new ChatMessage();
                userMsg.setSessionId(finalSessionId);
                userMsg.setRole("user");
                userMsg.setContent(content);
                messageMapper.insert(userMsg);

                // Update session update_time to bring it to top
                ChatSession sessionUpdate = new ChatSession();
                sessionUpdate.setId(finalSessionId);
                sessionUpdate.setUpdateTime(LocalDateTime.now());
                sessionMapper.updateById(sessionUpdate);

                // 3. Get Chat History for context
                List<ChatMessage> history = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, finalSessionId)
                        .orderByAsc(ChatMessage::getCreateTime));

                // 4. Call Python AI Service
                // Construct payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("message", content);

                List<Map<String, String>> historyList = new ArrayList<>();
                for (ChatMessage msg : history) {
                    if (!msg.getId().equals(userMsg.getId())) {
                        Map<String, String> m = new HashMap<>();
                        m.put("role", msg.getRole());
                        m.put("content", msg.getContent());
                        historyList.add(m);
                    }
                }
                payload.put("history", historyList);

                String jsonBody = objectMapper.writeValueAsString(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(PYTHON_SERVICE_URL)
                        .post(body)
                        .build();

                // Variable to hold citations if any
                final String[] citationsJson = { null };

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.error("Python service returned error: {}", response.code());
                        emitter.send(SseEmitter.event().data("Error from AI Service: " + response.code()));
                        emitter.complete();
                        return;
                    }

                    StringBuilder fullResponse = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data.trim())) {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                break;
                            }

                            // Check for citation event
                            if (data.trim().startsWith("{\"type\": \"citation\"")) {
                                try {
                                    Map<String, Object> eventMap = objectMapper.readValue(data, Map.class);
                                    if (eventMap.containsKey("citations")) {
                                        citationsJson[0] = objectMapper.writeValueAsString(eventMap.get("citations"));
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to parse citation event: {}", e.getMessage());
                                }
                                // Forward citation event to frontend
                                emitter.send(SseEmitter.event().data(data));
                                continue; // Do not append to fullResponse content
                            }

                            fullResponse.append(data);
                            // Forward to frontend
                            emitter.send(SseEmitter.event().data(data));
                        }
                    }

                    // 5. Save AI Response
                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setSessionId(finalSessionId);
                    aiMsg.setRole("assistant");
                    // Unescape newlines before saving to DB
                    String finalContent = fullResponse.toString().replace("\\n", "\n");
                    aiMsg.setContent(finalContent);
                    if (citationsJson[0] != null) {
                        aiMsg.setCitations(citationsJson[0]);
                    }
                    messageMapper.insert(aiMsg);

                    // Update session update_time again after AI responds
                    ChatSession sessionUpdateFinal = new ChatSession();
                    sessionUpdateFinal.setId(finalSessionId);
                    sessionUpdateFinal.setUpdateTime(LocalDateTime.now());
                    sessionMapper.updateById(sessionUpdateFinal);

                    emitter.complete();
                }

            } catch (Exception e) {
                log.error("AI Service Error", e);
                try {
                    emitter.send(SseEmitter.event().data("Error: " + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    public List<ChatSession> getSessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        // Delete messages first
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        // Delete session
        sessionMapper.deleteById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionTitle(Long sessionId, Long userId, String title) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackHistory(Long sessionId, Long userId, Integer count) {
        if (count == null || count <= 0)
            return;

        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        // 1. Get IDs of the last N messages
        List<ChatMessage> messagesToDelete = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + count));

        if (messagesToDelete.isEmpty())
            return;

        List<Long> ids = messagesToDelete.stream().map(ChatMessage::getId).toList();

        // 2. Delete them
        messageMapper.deleteBatchIds(ids);
    }
}
