# Privacy notes

SMS Relay has no app-controlled analytics, advertising, account system, or cloud backend. It processes SMS locally and encrypts queued sender and body values with an Android Keystore key. Completed records are eligible for deletion after 30 days. Android application backup is disabled.

When forwarding is enabled, the configured Bark service receives the Device Key, encrypted payload, IV, request timing, and ordinary network metadata over HTTPS. The receiving Bark app holds the matching payload key. The phone initially receives SMS in plaintext, and the operating system still controls notifications, screenshots, memory, and device-level diagnostics.

Clearing app storage or uninstalling the app removes its local queue and configuration under normal Android behavior. Copies retained by the push provider or receiving device are outside the app's control. Users are responsible for consent, lawful handling, recipient security, and provider retention settings.
