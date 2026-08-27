# Contributing

Bug reports, documentation improvements, and focused changes are welcome. Remove real SMS, recipient addresses, Device Keys, authorization codes, signing keys, and device identifiers before sharing anything. Describe Android version, steps, expected behavior, and redacted logs.

Before opening a pull request:

1. Run `./scripts/check-public-tree.ps1`.
2. Run `./build.ps1` and confirm host tests, APK signing, and manifest checks pass.
3. Explain user-visible, security, privacy, permission, and migration effects.
4. Keep production credentials and release keys outside the repository.
5. Document the source and license of every third-party dependency or asset, and preserve required notices.

Do not add permissions, providers, telemetry, or data collection without documenting their security and privacy impact. Do not change the package ID or Keystore alias without an explicit migration and security review.
