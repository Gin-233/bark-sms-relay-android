# SMS Relay — Bark-compatible

SMS Relay is a small Android app that receives SMS, stores pending messages in an encrypted local queue, and forwards them to Bark only when Android provides a validated Wi-Fi network.

This is an independent community project. It is not affiliated with, endorsed by, or operated by Bark.

[简体中文](README.zh-CN.md)

## Features

- Receives multipart SMS and reconciles the system inbox after restart or unlock.
- Encrypts queued sender and message values with an Android Keystore-backed AES-GCM key.
- Encrypts every Bark message body with AES-256-CBC and a fresh random IV.
- Sends only through a validated Wi-Fi network selected by Android; cellular transport is not used for delivery.
- Retries transient failures with bounded backoff and keeps completed local records for 30 days.
- Restores persisted work after reboot and sends an optional daily Bark health heartbeat.
- Contains no advertising, analytics, account system, or app-controlled cloud backend.

The manifest requests SMS receive/read, internet, network-state, boot-complete, and Android 13+ notification permissions. It does not request contacts, location, camera, microphone, or storage access.

## Requirements

- Android 8.0 or later
- A Bark server reachable over Wi-Fi and a Bark Device Key
- Windows PowerShell 5.1+ or PowerShell 7 for the included build script
- JDK 17, Android SDK platform 35, and Windows Android build-tools

## Build

From the repository root:

```powershell
.\build.ps1
```

The script runs host tests, compiles the Android app without Gradle, verifies the APK signature and manifest, and writes `build/outputs/sms-relay-<version>.apk`.

A standalone clone creates an ignored local debug key under `.keys/`. GitHub Actions uses a disposable debug key. Create and protect a dedicated signing key before distributing a release, and never commit that key or its passwords.

## Configure Bark

1. Install Bark on the receiving device and copy only its Device Key.
2. In SMS Relay, open **Bark & encryption**, enter the HTTPS Bark server and Device Key, then save.
3. Copy the generated AES key and fallback IV into Bark's push-encryption settings using AES256, CBC, and PKCS7.
4. Use the in-app Bark test on non-production data before relying on unattended forwarding.

The app uses package ID `com.local.smsrelay` and a dedicated Keystore alias. Android isolates it from apps that use a different application ID, and no cross-application migration is performed. Forks should choose an application ID they control before store distribution.

## Security and limitations

SMS and verification codes are highly sensitive. Use this app only on a device and messages you are authorized to process. The app reduces exposure at rest and in transit, but it cannot protect a rooted, unlocked, malware-infected, or otherwise compromised phone. Notifications, screenshots, metadata, timing, provider logs, and the receiving device can still disclose information.

Wi-Fi loss, Android power management, revoked Bark credentials, or provider outages can delay delivery. Review [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), and the [release checklist](OPEN_SOURCE_CHECKLIST.md) before deployment.

## License

Original project code and documentation are available under the [MIT License](LICENSE). Adapted Google Material Icons remain under Apache-2.0, and the Bark encryption test vector retains Bark's MIT notice. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [LICENSES](LICENSES/).
