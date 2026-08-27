package com.local.smsrelay;

import java.net.URI;

final class ChannelConfigValidator {
    private ChannelConfigValidator() {}

    static String normalizeBarkServer(String server) {
        String normalized = clean(server);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            URI uri = new URI(normalized);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("Bark 服务器必须是有效的 HTTPS 地址");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bark 服务器地址格式不正确");
        }
        return normalized;
    }

    static void validateBarkDeviceKey(String key) {
        String value = clean(key);
        if (value.isEmpty()) {
            return;
        }
        if (value.contains("://") || containsWhitespace(value)
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("这里只能填写 Bark Device Key，不能填写完整 URL");
        }
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
