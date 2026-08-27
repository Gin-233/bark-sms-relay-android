package com.local.smsrelay;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class DeliveryText {
    private static final DateTimeFormatter FULL = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT = DateTimeFormatter
            .ofPattern("MM-dd HH:mm", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private DeliveryText() {}

    static String smsTitle(SmsRecord record) {
        return "短信 · " + record.sender;
    }

    static String smsBody(SmsRecord record) {
        StringBuilder result = new StringBuilder();
        result.append("时间：").append(formatFullTime(record.receivedAt)).append('\n');
        result.append("发件：").append(record.sender).append('\n');
        if (record.simSlot >= 0) {
            result.append("SIM：").append(record.simSlot + 1).append('\n');
        }
        result.append("编号：").append(record.id).append("\n\n");
        result.append(record.body);
        return result.toString();
    }

    static String heartbeatBody(int pending, int batteryPercent,
                                String network, long uptimeMillis) {
        long uptimeHours = uptimeMillis / (60L * 60 * 1000);
        return "短信安全转发服务正在运行。\n"
                + "时间：" + formatFullTime(System.currentTimeMillis()) + "\n"
                + "网络：" + network + "\n"
                + "电量：" + (batteryPercent >= 0 ? batteryPercent + "%" : "未知") + "\n"
                + "本次开机：约 " + uptimeHours + " 小时\n"
                + "Bark 待发：" + pending;
    }

    static String formatFullTime(long timestamp) {
        return FULL.format(Instant.ofEpochMilli(timestamp));
    }

    static String formatShortTime(long timestamp) {
        return SHORT.format(Instant.ofEpochMilli(timestamp));
    }
}
