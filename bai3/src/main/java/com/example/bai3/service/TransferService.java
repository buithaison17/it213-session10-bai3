package com.example.bai3.service;

import com.example.bai3.config.LangfuseProperties;
import com.example.bai3.util.MaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final RestClient restClient;

    public TransferService(LangfuseProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getPublicKey(), properties.getSecretKey()))
                .build();
    }

    public void processTransfer(String sessionId, String userId, String toAccount, double amount) {
        String traceId = UUID.randomUUID().toString();
        String maskedUser = MaskingUtils.maskUsername(userId);
        String maskedAccount = MaskingUtils.maskAccountNumber(toAccount);

        Map<String, Object> inputData = Map.of(
                "action", "TRANSFER",
                "fromUserMasked", maskedUser,
                "toAccountMasked", maskedAccount,
                "amountTier", amount < 1_000_000 ? "< 1M" : ">= 1M"
        );

        try {
            log.info("Processing transfer for user: {} to account: {}", maskedUser, maskedAccount);
            executeTransferLogic(userId, toAccount, amount);

            Map<String, Object> outputData = Map.of(
                    "status", "SUCCESS",
                    "message", String.format("Transfer completed from %s to %s", maskedUser, maskedAccount)
            );
            sendTrace(traceId, sessionId, userId, inputData, outputData);

        } catch (Exception ex) {
            log.error("Transfer failed: {}", ex.getMessage(), ex);
            Map<String, Object> errorOutput = Map.of(
                    "status", "FAILED",
                    "errorCode", "TRANSFER_ERROR",
                    "errorMessage", ex.getMessage()
            );
            sendTrace(traceId, sessionId, userId, inputData, errorOutput);
            throw ex;
        }
    }

    private void sendTrace(String traceId, String sessionId, String userId,
                           Map<String, Object> input, Map<String, Object> output) {
        try {
            Map<String, Object> traceBody = Map.of(
                    "id", traceId,
                    "name", "bank-transfer",
                    "userId", userId,
                    "sessionId", sessionId,
                    "input", input,
                    "output", output,
                    "metadata", Map.of(
                            "service", "TransferService",
                            "environment", "production"
                    ),
                    "timestamp", Instant.now().toString()
            );

            Map<String, Object> event = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "trace-create",
                    "timestamp", Instant.now().toString(),
                    "body", traceBody
            );

            restClient.post()
                    .uri("/api/public/ingestion")
                    .body(Map.of("batch", List.of(event)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Telemetry failure: {}", e.getMessage());
        }
    }

    private void executeTransferLogic(String user, String toAccount, double amount) {
    }
}
