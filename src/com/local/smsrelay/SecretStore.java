package com.local.smsrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecretStore {
    private static final String KEY_ALIAS = "sms_relay_master_v1";
    private static final String PREFS = "secure_values_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final Object KEY_LOCK = new Object();

    private final SharedPreferences preferences;

    SecretStore(Context context) {
        Context deviceContext = RelayApp.deviceContext(context);
        preferences = deviceContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void put(String name, String value) {
        putAll(Collections.singletonMap(name, value));
    }

    synchronized void putAll(Map<String, String> values) {
        try {
            SecretKey key = getOrCreateKey();
            Map<String, String> encrypted = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty()) {
                    throw new IllegalArgumentException("Secure value name is empty");
                }
                encrypted.put(entry.getKey(), entry.getValue() == null
                        ? null : encryptValue(key, entry.getValue()));
            }
            SharedPreferences.Editor editor = preferences.edit();
            for (Map.Entry<String, String> entry : encrypted.entrySet()) {
                if (entry.getValue() == null) {
                    editor.remove(entry.getKey());
                } else {
                    editor.putString(entry.getKey(), entry.getValue());
                }
            }
            if (!editor.commit()) {
                throw new IllegalStateException("Secure preferences commit failed");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Secure value could not be stored", e);
        }
    }

    synchronized String get(String name) {
        String encoded = preferences.getString(name, null);
        if (encoded == null) {
            return null;
        }
        try {
            String[] parts = encoded.split("\\.", 3);
            if (parts.length != 3 || !"1".equals(parts[0])) {
                throw new IllegalStateException("Unknown secure value format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Secure value could not be read", e);
        }
    }

    synchronized boolean has(String name) {
        return preferences.contains(name);
    }

    synchronized void remove(String name) {
        preferences.edit().remove(name).apply();
    }

    String encryptForDatabase(String plaintext) {
        if (plaintext == null) {
            plaintext = "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = requireGeneratedIv(cipher);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + "."
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("SMS could not be encrypted", e);
        }
    }

    String decryptFromDatabase(String encoded) {
        try {
            String[] parts = encoded.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid encrypted SMS format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SMS could not be decrypted", e);
        }
    }

    private static byte[] requireGeneratedIv(Cipher cipher) {
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != 12) {
            throw new IllegalStateException("Android Keystore did not generate a 96-bit GCM IV");
        }
        return iv;
    }

    private static String encryptValue(SecretKey key, String value) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = requireGeneratedIv(cipher);
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return "1." + Base64.getEncoder().encodeToString(iv) + "."
                + Base64.getEncoder().encodeToString(ciphertext);
    }

    private SecretKey getOrCreateKey() throws Exception {
        synchronized (KEY_LOCK) {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry existing = keyStore.getEntry(KEY_ALIAS, null);
            if (existing instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) existing).getSecretKey();
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            keyGenerator.init(spec);
            return keyGenerator.generateKey();
        }
    }
}
