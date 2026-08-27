package com.local.smsrelay;

import android.net.Network;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

final class BarkClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final int MAX_RESPONSE_BYTES = 8_192;

    private final Config config;

    BarkClient(Config config) {
        this.config = config;
    }

    void sendOnNetwork(Network network, String title, String body, String group,
                       String level, String id) throws Exception {
        if (network == null) {
            throw new IllegalArgumentException("Wi-Fi network is unavailable");
        }
        sendBound(network, title, body, group, level, id);
    }

    private void sendBound(Network network, String title, String body, String group,
                           String level, String id) throws Exception {
        String key = config.barkAesKey();
        String deviceKey = config.barkDeviceKey();
        if (key == null || deviceKey == null) {
            throw new IllegalStateException("Bark is not configured");
        }

        String iv = CryptoUtils.randomAscii(16);
        JSONObject inner = new JSONObject();
        inner.put("title", title);
        inner.put("body", body);
        inner.put("group", group);
        inner.put("level", level);
        inner.put("isArchive", "1");
        inner.put("id", id);
        String ciphertext = CryptoUtils.encryptBark(inner.toString(), key, iv);

        JSONObject request = new JSONObject();
        request.put("device_key", deviceKey);
        request.put("ciphertext", ciphertext);
        request.put("iv", iv);
        request.put("id", id);

        URL url = new URL(config.barkServer() + "/push");
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalArgumentException("Bark endpoint is not HTTPS");
        }
        HttpsURLConnection connection = (HttpsURLConnection) network.openConnection(url);
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "SmsRelay/1.0.0");
            byte[] payload = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int httpCode = connection.getResponseCode();
            String response = readLimited(
                    httpCode >= 200 && httpCode < 400
                            ? connection.getInputStream() : connection.getErrorStream());
            if (httpCode < 200 || httpCode >= 300) {
                throw new IllegalStateException("Bark HTTP " + httpCode);
            }
            if (!response.isEmpty()) {
                JSONObject json = new JSONObject(response);
                if (json.has("code") && json.optInt("code", -1) != 200) {
                    throw new IllegalStateException(
                            "Bark rejected request: code=" + json.optInt("code", -1));
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readLimited(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int total = 0;
            int count;
            while ((count = source.read(buffer)) != -1 && total < MAX_RESPONSE_BYTES) {
                int allowed = Math.min(count, MAX_RESPONSE_BYTES - total);
                output.write(buffer, 0, allowed);
                total += allowed;
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
