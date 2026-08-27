package com.local.smsrelay;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserManager;

final class SmsImporter {
    private static final long OVERLAP_MS = 5 * 60_000L;

    private SmsImporter() {}

    static int importRecent(Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null || !userManager.isUserUnlocked()) {
            return 0;
        }
        if (context.checkSelfPermission(Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return 0;
        }

        Config config = new Config(context);
        long since = Math.max(config.installedAt(), config.lastInboxSync() - OVERLAP_MS);
        int inserted = 0;
        try (Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date", "date_sent"},
                "date>=?", new String[]{Long.toString(since)}, "date ASC")) {
            if (cursor == null) {
                return 0;
            }
            SmsStore store = SmsStore.get(context);
            while (cursor.moveToNext()) {
                long eventTimestamp = cursor.getLong(4);
                if (eventTimestamp <= 0L) {
                    eventTimestamp = cursor.getLong(3);
                }
                if (store.insert(cursor.getString(1), cursor.getString(2),
                        eventTimestamp, -1)) {
                    inserted++;
                }
            }
            config.setLastInboxSync(System.currentTimeMillis());
        } catch (RuntimeException ignored) {
            return inserted;
        }
        if (inserted > 0) {
            Scheduler.scheduleImmediate(context);
        }
        return inserted;
    }
}
