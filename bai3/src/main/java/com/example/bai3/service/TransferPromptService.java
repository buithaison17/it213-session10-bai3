package com.example.bai3.service;

import com.example.bai3.config.LangfuseProperties;
import com.example.bai3.dto.TransferIntentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class TransferPromptService {
    private static final Logger log = LoggerFactory.getLogger(TransferPromptService.class);
    private final ChatClient chatClient;
    private final RestClient langfuseRestClient;
    private final ObjectMapper objectMapper;

    public TransferPromptService(ChatClient.Builder chatClientBuilder,
                                 LangfuseProperties properties,
                                 ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.langfuseRestClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getPublicKey(), properties.getSecretKey()))
                .build();
    }

    public String getPromptFromRegistry(String promptName, String label) {
        try {
            log.info("Lấy prompt '{}' với nhãn '{}' từ Langfuse Registry...", promptName, label);
            String response = langfuseRestClient.get()
                    .uri("/api/public/v2/prompts/{name}?label={label}", promptName, label)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("prompt").asText();
        } catch (Exception e) {
            log.warn("Không thể lấy prompt từ registry ({}), sử dụng fallback prompt.", e.getMessage());
            return getDefaultFallbackPrompt();
        }
    }

    public String compilePrompt(String template, Map<String, Object> variables) {
        String compiled = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            compiled = compiled.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return compiled;
    }

    public TransferIntentResponse extractTransferIntent(String senderName, double currentBalance, String userInput) {
        String template = getPromptFromRegistry("bank-transfer-extractor", "production");

        Map<String, Object> variables = Map.of(
                "sender_name", senderName,
                "current_balance", String.format("%.0f", currentBalance),
                "user_input", userInput
        );

        String finalPrompt = compilePrompt(template, variables);

        String rawResponse = chatClient.prompt()
                .user(finalPrompt)
                .call()
                .content();

        log.info("Raw response từ LLM: {}", rawResponse);

        String cleanJson = cleanMarkdownJson(rawResponse);

        try {
            return objectMapper.readValue(cleanJson, TransferIntentResponse.class);
        } catch (Exception e) {
            log.error("Lỗi parse JSON: {}", e.getMessage());
            return new TransferIntentResponse("FAILED_PARSE", null, null, null, null, "", "Không thể phân tích dữ liệu JSON");
        }
    }

    private String cleanMarkdownJson(String raw) {
        if (raw == null) return "{}";
        String clean = raw.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    private String getDefaultFallbackPrompt() {
        return "Bạn là trợ lý ngân hàng. Trích xuất thông tin chuyển khoản từ câu lệnh: '{{user_input}}' của người dùng {{sender_name}} (Số dư: {{current_balance}} VND). Trả về JSON chuẩn schema.";
    }
}
