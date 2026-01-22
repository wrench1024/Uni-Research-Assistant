package com.uni.research.module.analysis.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

public interface AnalysisService {
    /**
     * Analyze a single document (e.g., Summary)
     * 
     * @param userId Current user ID
     * @param docId  Document ID
     * @param type   Analysis type
     * @return SSE Emitter
     */
    SseEmitter analyzeSummary(Long userId, String docId, String type);

    /**
     * Compare multiple documents
     * 
     * @param userId  Current user ID
     * @param docIds  List of Document IDs
     * @param aspects Aspects to compare
     * @return SSE Emitter
     */
    SseEmitter analyzeComparison(Long userId, List<String> docIds, List<String> aspects);
}
