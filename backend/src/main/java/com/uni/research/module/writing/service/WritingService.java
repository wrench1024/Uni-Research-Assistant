package com.uni.research.module.writing.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface WritingService {
    /**
     * Process text for writing assistance
     * 
     * @param userId      Current user ID
     * @param text        Text to process
     * @param instruction Instruction (polish, expand, etc.)
     * @param context     Optional context
     * @return SSE Emitter
     */
    SseEmitter processText(Long userId, String text, String instruction, String context);
}
