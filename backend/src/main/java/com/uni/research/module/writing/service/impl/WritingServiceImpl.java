package com.uni.research.module.writing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.research.module.writing.service.WritingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(Duration.ofMinutes(3))
            .build();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String PYTHON_WRITE_URL = "http://localhost:8000/api/v1/write/process";

    @Override
    public SseEmitter processText(Long userId, String text, String instruction, String context) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes

        executor.execute(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("text", text);
                payload.put("instruction", instruction);
                if (context != null) {
                    payload.put("context", context);
                }

                String jsonBody = objectMapper.writeValueAsString(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(PYTHON_WRITE_URL)
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
                            emitter.send(SseEmitter.event().data(data));
                        }
                    }
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Writing Service Error", e);
                try {
                    emitter.send(SseEmitter.event().data("Error: " + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
