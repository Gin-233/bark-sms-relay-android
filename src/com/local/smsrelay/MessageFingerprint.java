package com.local.smsrelay;

final class MessageFingerprint {
    private MessageFingerprint() {}

    static String create(String sender, String body, long receivedAt) {
        String safeSender = sender == null || sender.trim().isEmpty()
                ? "未知号码" : sender.trim();
        String safeBody = body == null ? "" : body;
        return CryptoUtils.sha256Hex(
                safeSender + "\u0000" + receivedAt + "\u0000" + safeBody);
    }
}
