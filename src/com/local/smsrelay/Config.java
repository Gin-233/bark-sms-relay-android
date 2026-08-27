package com.local.smsrelay;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

final class Config {
    private static final String PREFS = "relay_config_v1";
    private static final String DEFAULT_BARK_SERVER = "https://api.day.app";

    private static final String S_BARK_KEY = "bark_device_key";
    private static final String S_BARK_AES_KEY = "bark_aes_key";
    private static final String S_BARK_FALLBACK_IV = "bark_fallback_iv";
    private static final String S_BARK_SERVER = "bark_server_v2";

    private final SharedPreferences preferences;
    private final SecretStore secrets;

    Config(Context context) {
        Context deviceContext = RelayApp.deviceContext(context);
        this.preferences = deviceContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.secrets = new SecretStore(deviceContext);
    }

    static void ensureInitialized(Context context) {
        Config config = new Config(context);
        if (!config.preferences.contains("installed_at")) {
            config.preferences.edit()
                    .putLong("installed_at", System.currentTimeMillis())
                    .putBoolean("enabled", true)
                    .putString("bark_server", DEFAULT_BARK_SERVER)
                    .apply();
        }
    }

    boolean enabled() {
        return preferences.getBoolean("enabled", true);
    }

    void setEnabled(boolean enabled) {
        preferences.edit().putBoolean("enabled", enabled).apply();
    }

    String barkServer() {
        String secured = safeGet(S_BARK_SERVER);
        return nonEmpty(secured)
                ? secured : preferences.getString("bark_server", DEFAULT_BARK_SERVER);
    }

    void saveBark(String server, String barkKey) {
        String normalizedServer = ChannelConfigValidator.normalizeBarkServer(server);
        String cleanBarkKey = clean(barkKey);
        ChannelConfigValidator.validateBarkDeviceKey(cleanBarkKey);

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put(S_BARK_SERVER, normalizedServer);
        if (!secrets.has(S_BARK_AES_KEY)) {
            updates.put(S_BARK_AES_KEY, CryptoUtils.randomAscii(32));
        }
        if (!secrets.has(S_BARK_FALLBACK_IV)) {
            updates.put(S_BARK_FALLBACK_IV, CryptoUtils.randomAscii(16));
        }
        if (!cleanBarkKey.isEmpty()) {
            updates.put(S_BARK_KEY, cleanBarkKey);
        }
        secrets.putAll(updates);
    }

    void ensureBarkCrypto() {
        Map<String, String> missing = new LinkedHashMap<>();
        if (!secrets.has(S_BARK_AES_KEY)) {
            missing.put(S_BARK_AES_KEY, CryptoUtils.randomAscii(32));
        }
        if (!secrets.has(S_BARK_FALLBACK_IV)) {
            missing.put(S_BARK_FALLBACK_IV, CryptoUtils.randomAscii(16));
        }
        if (!missing.isEmpty()) {
            secrets.putAll(missing);
        }
    }

    boolean barkReady() {
        return nonEmpty(barkDeviceKey()) && nonEmpty(barkAesKey())
                && nonEmpty(barkFallbackIv()) && barkServer().startsWith("https://");
    }

    String barkDeviceKey() { return safeGet(S_BARK_KEY); }
    String barkAesKey() { return safeGet(S_BARK_AES_KEY); }
    String barkFallbackIv() { return safeGet(S_BARK_FALLBACK_IV); }

    long installedAt() {
        return preferences.getLong("installed_at", System.currentTimeMillis());
    }

    long lastInboxSync() {
        return preferences.getLong("last_inbox_sync", installedAt());
    }

    void setLastInboxSync(long timestamp) {
        preferences.edit().putLong("last_inbox_sync", timestamp).apply();
    }

    long lastBarkHeartbeatAt() {
        return preferences.getLong("last_bark_heartbeat_at", 0L);
    }

    void setLastBarkHeartbeatAt(long timestamp) {
        preferences.edit().putLong("last_bark_heartbeat_at", timestamp).apply();
    }

    private String safeGet(String name) {
        try {
            return secrets.get(name);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean nonEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
