package com.example.bai3.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.bai3.dto.TransferIntentResponse;
import com.example.bai3.service.TransferPromptService;
import com.example.bai3.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {
    private final TransferPromptService promptService;
    private final TransferService transferService;

    public TransferController(TransferPromptService promptService, TransferService transferService) {
        this.promptService = promptService;
        this.transferService = transferService;
    }

    @PostMapping("/parse-intent")
    public ResponseEntity<TransferIntentResponse> parseTransferIntent(@RequestBody Map<String, Object> payload) {
        String senderName = (String) payload.getOrDefault("senderName", "Demo User");
        double balance = Double.parseDouble(payload.getOrDefault("balance", "5000000").toString());
        String message = (String) payload.getOrDefault("message", "");

        TransferIntentResponse response = promptService.extractTransferIntent(senderName, balance, message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<String> executeTransfer(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.getOrDefault("sessionId", "sess-01");
        String userId = (String) payload.getOrDefault("userId", "user-01");
        String toAccount = (String) payload.getOrDefault("toAccount", "0123456789");
        double amount = Double.parseDouble(payload.getOrDefault("amount", "100000").toString());

        transferService.processTransfer(sessionId, userId, toAccount, amount);
        return ResponseEntity.ok("Transfer completed and traced successfully");
    }
}
