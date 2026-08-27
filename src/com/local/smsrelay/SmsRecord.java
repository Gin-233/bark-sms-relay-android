package com.local.smsrelay;

final class SmsRecord {
    final String id;
    final long receivedAt;
    final String sender;
    final String body;
    final int simSlot;
    final boolean barkSent;
    final int barkAttempts;

    SmsRecord(String id, long receivedAt, String sender, String body, int simSlot,
              boolean barkSent, int barkAttempts) {
        this.id = id;
        this.receivedAt = receivedAt;
        this.sender = sender;
        this.body = body;
        this.simSlot = simSlot;
        this.barkSent = barkSent;
        this.barkAttempts = barkAttempts;
    }
}
