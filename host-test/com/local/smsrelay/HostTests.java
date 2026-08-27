package com.local.smsrelay;

public final class HostTests {
    public static void main(String[] args) throws Exception {
        testOfficialBarkVector();
        testRetryPolicy();
        testMessageFingerprintPrecision();
        testChannelValidation();
        testScheduleCoalescing();
        System.out.println("Host tests passed");
    }

    private static void testOfficialBarkVector() throws Exception {
        // Published Bark encryption vector, used under Bark's MIT license.
        // See THIRD_PARTY_NOTICES.md and LICENSES/Bark-MIT.txt.
        String plaintext = "{\"body\": \"test\", \"sound\": \"birdsong\"}";
        String encrypted = CryptoUtils.encryptBark(
                plaintext, "1234567890123456", "1111111111111111");
        String expected = "d3QhjQjP5majvNt5CjsvFWwqqj2gKl96RFj5OO+u6ynTt7lkyigDYNA3abnnCLpr";
        require(expected.equals(encrypted), "Bark AES-CBC vector mismatch: " + encrypted);
    }

    private static void testRetryPolicy() {
        require(RetryPolicy.delayAfterFailure(1) == 60_000L, "first retry delay");
        require(RetryPolicy.delayAfterFailure(5) == 30 * 60_000L, "fifth retry delay");
        require(RetryPolicy.delayAfterFailure(99) == 6 * 60 * 60_000L, "retry cap");
    }

    private static void testMessageFingerprintPrecision() {
        String first = MessageFingerprint.create("service", "code 123456", 1_000L);
        String same = MessageFingerprint.create("service", "code 123456", 1_000L);
        String repeated = MessageFingerprint.create("service", "code 123456", 31_000L);
        require(first.equals(same), "same SMS event must deduplicate");
        require(!first.equals(repeated),
                "identical SMS messages at different timestamps must both be retained");
    }

    private static void testChannelValidation() {
        require("https://api.day.app".equals(
                        ChannelConfigValidator.normalizeBarkServer(" https://api.day.app/ ")),
                "Bark server normalization");

        boolean insecureServerBlocked = false;
        try {
            ChannelConfigValidator.normalizeBarkServer("http://api.day.app");
        } catch (IllegalArgumentException expected) {
            insecureServerBlocked = true;
        }
        require(insecureServerBlocked, "non-HTTPS Bark servers must be rejected");

        boolean fullUrlBlocked = false;
        try {
            ChannelConfigValidator.validateBarkDeviceKey("https://api.day.app/device-key");
        } catch (IllegalArgumentException expected) {
            fullUrlBlocked = true;
        }
        require(fullUrlBlocked, "full Bark URL must not be accepted as a device key");
    }

    private static void testScheduleCoalescing() {
        require(SchedulePolicy.shouldReplace(20_000L, 10_000L),
                "earlier delivery must replace a later job");
        require(!SchedulePolicy.shouldReplace(10_000L, 20_000L),
                "later delivery must not push back an existing job");
        require(SchedulePolicy.shouldReplace(0L, 10_000L),
                "legacy job without due metadata must be replaced once");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
