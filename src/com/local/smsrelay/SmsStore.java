package com.local.smsrelay;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class SmsStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sms_relay.db";
    private static final int DATABASE_VERSION = 1;
    private static final long RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    private static volatile SmsStore instance;

    private final SecretStore secrets;

    private SmsStore(Context context) {
        super(RelayApp.deviceContext(context), DATABASE_NAME, null, DATABASE_VERSION);
        secrets = new SecretStore(context);
        setWriteAheadLoggingEnabled(true);
    }

    static SmsStore get(Context context) {
        SmsStore current = instance;
        if (current == null) {
            synchronized (SmsStore.class) {
                current = instance;
                if (current == null) {
                    current = new SmsStore(context.getApplicationContext());
                    instance = current;
                }
            }
        }
        return current;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages ("
                + "id TEXT PRIMARY KEY,"
                + "fingerprint TEXT NOT NULL UNIQUE,"
                + "received_at INTEGER NOT NULL,"
                + "sender_enc TEXT NOT NULL,"
                + "body_enc TEXT NOT NULL,"
                + "sim_slot INTEGER NOT NULL DEFAULT -1,"
                + "bark_sent INTEGER NOT NULL DEFAULT 0,"
                + "bark_attempts INTEGER NOT NULL DEFAULT 0,"
                + "next_bark_at INTEGER NOT NULL DEFAULT 0,"
                + "last_bark_error TEXT,"
                + "created_at INTEGER NOT NULL,"
                + "completed_at INTEGER"
                + ")");
        db.execSQL("CREATE INDEX messages_due_idx ON messages(bark_sent, next_bark_at)");
        db.execSQL("CREATE INDEX messages_received_idx ON messages(received_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported SMS database migration target");
    }

    boolean insert(String sender, String body, long receivedAt, int simSlot) {
        String safeSender = sender == null || sender.trim().isEmpty() ? "未知号码" : sender.trim();
        String safeBody = body == null ? "" : body;
        String fingerprint = MessageFingerprint.create(safeSender, safeBody, receivedAt);

        ContentValues values = new ContentValues();
        values.put("id", UUID.randomUUID().toString());
        values.put("fingerprint", fingerprint);
        values.put("received_at", receivedAt);
        values.put("sender_enc", secrets.encryptForDatabase(safeSender));
        values.put("body_enc", secrets.encryptForDatabase(safeBody));
        values.put("sim_slot", simSlot);
        values.put("created_at", System.currentTimeMillis());
        long row = getWritableDatabase().insertWithOnConflict(
                "messages", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return row != -1L;
    }

    List<SmsRecord> due(long now, int limit) {
        List<SmsRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "messages",
                new String[]{"id", "received_at", "sender_enc", "body_enc", "sim_slot",
                        "bark_sent", "bark_attempts"},
                "bark_sent=0 AND next_bark_at<=?",
                new String[]{Long.toString(now)}, null, null,
                "received_at ASC", Integer.toString(limit))) {
            while (cursor.moveToNext()) {
                records.add(new SmsRecord(
                        cursor.getString(0),
                        cursor.getLong(1),
                        secrets.decryptFromDatabase(cursor.getString(2)),
                        secrets.decryptFromDatabase(cursor.getString(3)),
                        cursor.getInt(4),
                        cursor.getInt(5) != 0,
                        cursor.getInt(6)));
            }
        }
        return records;
    }

    void markBarkSuccess(String id) {
        ContentValues values = new ContentValues();
        values.put("bark_sent", 1);
        values.putNull("last_bark_error");
        values.put("completed_at", System.currentTimeMillis());
        getWritableDatabase().update("messages", values, "id=?", new String[]{id});
    }

    void markBarkFailure(String id, int attempts, long nextAt, String error) {
        ContentValues values = new ContentValues();
        values.put("bark_attempts", attempts);
        values.put("next_bark_at", nextAt);
        values.put("last_bark_error", truncate(error, 240));
        getWritableDatabase().update("messages", values, "id=?", new String[]{id});
    }

    void deferBark(String id, long nextAt) {
        ContentValues values = new ContentValues();
        values.put("next_bark_at", nextAt);
        getWritableDatabase().update("messages", values, "id=?", new String[]{id});
    }

    long earliestDue() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT MIN(next_bark_at) FROM messages WHERE bark_sent=0", null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
            return -1L;
        }
    }

    int pendingCount() {
        return count("bark_sent=0");
    }

    int completedCount() {
        return count("bark_sent=1");
    }

    List<String> recentStatus(int limit) {
        List<String> lines = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "messages",
                new String[]{"id", "received_at", "sender_enc", "bark_sent",
                        "bark_attempts"},
                null, null, null, null, "received_at DESC", Integer.toString(limit))) {
            while (cursor.moveToNext()) {
                String sender;
                try {
                    sender = maskSender(secrets.decryptFromDatabase(cursor.getString(2)));
                } catch (RuntimeException e) {
                    sender = "[无法解密]";
                }
                lines.add(String.format(Locale.ROOT,
                        "%s  %s  Bark:%s(%d)  #%s",
                        DeliveryText.formatShortTime(cursor.getLong(1)),
                        sender,
                        cursor.getInt(3) != 0 ? "成功" : "待发",
                        cursor.getInt(4),
                        cursor.getString(0).substring(0, 8)));
            }
        }
        return lines;
    }

    void cleanup() {
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        getWritableDatabase().delete(
                "messages", "bark_sent=1 AND completed_at<?",
                new String[]{Long.toString(cutoff)});
    }

    private int count(String selection) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM messages WHERE " + selection, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "unknown error";
        }
        String singleLine = value.replace('\r', ' ').replace('\n', ' ');
        return singleLine.length() <= max ? singleLine : singleLine.substring(0, max);
    }

    private static String maskSender(String sender) {
        if (sender == null || sender.length() <= 4) {
            return "****";
        }
        return "***" + sender.substring(sender.length() - 4);
    }
}
