package com.uni.research.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG Service Client
 * 调用 Python AI 服务的 RAG 相关 API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RestTemplate restTemplate;

    @Value("${ai-service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * 异步触发文档索引
     * 在文档上传后调用，将文档内容向量化存储到 pgvector
     *
     * @param filePath 文档在 MinIO 中的路径（或本地临时路径）
     * @param docId    文档唯一标识
     */
    @Async
    public void triggerDocumentIngestion(String filePath, String docId) {
        log.info("触发文档索引: docId={}, filePath={}", docId, filePath);

        try {
            String url = aiServiceBaseUrl + "/api/v1/ingest/path";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("file_path", filePath);
            requestBody.put("doc_id", docId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("文档索引成功: docId={}, response={}", docId, response.getBody());
            } else {
                log.warn("文档索引失败: docId={}, status={}", docId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("文档索引异常: docId={}, error={}", docId, e.getMessage(), e);
            // 异步操作，不抛出异常，只记录日志
        }
    }

    /**
     * 检查 RAG 服务是否可用
     */
    public boolean isRagServiceAvailable() {
        try {
            String url = aiServiceBaseUrl + "/";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null) {
                Object ragEnabled = response.getBody().get("rag_enabled");
                return Boolean.TRUE.equals(ragEnabled);
            }
            return false;
        } catch (Exception e) {
            log.warn("RAG 服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 异步删除文档向量
     * 在文档删除时调用，从 pgvector 中删除对应的向量
     *
     * @param docId 文档唯一标识
     */
    @Async
    public void deleteDocumentVectors(String docId) {
        log.info("删除文档向量: docId={}", docId);

        try {
            String url = aiServiceBaseUrl + "/api/v1/vectors/" + docId;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("文档向量删除成功: docId={}, response={}", docId, response.getBody());
            } else {
                log.warn("文档向量删除失败: docId={}, status={}", docId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("文档向量删除异常: docId={}, error={}", docId, e.getMessage(), e);
            // 异步操作，不抛出异常，只记录日志
        }
    }
}
