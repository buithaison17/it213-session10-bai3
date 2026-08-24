package com.example.bai3.util;

public class MaskingUtils {
    private MaskingUtils() {
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int maskedCount = accountNumber.length() - 4;
        return "*".repeat(maskedCount) + accountNumber.substring(maskedCount);
    }

    public static String maskUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "***";
        }
        String trimmed = username.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
    }
}
