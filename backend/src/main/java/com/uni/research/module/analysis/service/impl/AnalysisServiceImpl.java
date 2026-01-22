package com.uni.research.module.analysis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.research.module.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(Duration.ofMinutes(5)) // Long timeout for analysis
            .build();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String PYTHON_BASE_URL = "http://localhost:8000/api/v1/analyze";

    @Override
    public SseEmitter analyzeSummary(Long userId, String docId, String type) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes

        executor.execute(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("doc_id", docId);
                payload.put("type", type);

                String url = PYTHON_BASE_URL + "/summary";
                streamRequest(url, payload, emitter);

            } catch (Exception e) {
                log.error("Analysis Summary Error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter analyzeComparison(Long userId, List<String> docIds, List<String> aspects) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes

        executor.execute(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("doc_ids", docIds);
                if (aspects != null) {
                    payload.put("aspects", aspects);
                }

                String url = PYTHON_BASE_URL + "/comparison";
                streamRequest(url, payload, emitter);

            } catch (Exception e) {
                log.error("Analysis Comparison Error", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void streamRequest(String url, Map<String, Object> payload, SseEmitter emitter) {
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    emitter.send(SseEmitter.event().data("Error from AI Service: " + response.code()));
                    emitter.complete();
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data.trim())) {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            break;
                        }
                        // Forward to frontend
                        emitter.send(SseEmitter.event().data(data));
                    }
                }
                emitter.complete();
            }
        } catch (Exception e) {
            log.error("Stream Request Error", e);
            try {
                emitter.send(SseEmitter.event().data("Error: " + e.getMessage()));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(e);
        }
    }
}
