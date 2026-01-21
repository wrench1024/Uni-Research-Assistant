package com.uni.research.module.chat.dto;

import lombok.Data;

@Data
public class ChatSendRequest {
    /**
     * 会话ID，如果是新会话则为 null
     */
    private Long sessionId;

    /**
     * 用户发送的消息内容
     */
    private String content;
}
