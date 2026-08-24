package com.example.bai3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record TransferIntentResponse(
        @JsonProperty("status") String status,
        @JsonProperty("recipient_account") String recipientAccount,
        @JsonProperty("recipient_name") String recipientName,
        @JsonProperty("bank_code") String bankCode,
        @JsonProperty("amount") Double amount,
        @JsonProperty("transfer_note") String transferNote,
        @JsonProperty("reason") String reason
) {
}
