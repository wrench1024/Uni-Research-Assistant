package com.uni.research.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /**
     * 角色：user/assistant
     */
    private String role;

    private String content;

    private Integer tokenCount;

    /**
     * 引用信息 (JSON格式)
     */
    private String citations;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
