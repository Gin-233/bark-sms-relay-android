# Open-source release checklist

Use this checklist before creating or updating a public repository.

- Work only inside this standalone repository. Never publish a parent device-maintenance workspace.
- Run `./scripts/check-public-tree.ps1` and `./build.ps1` from a clean checkout.
- Review the complete tracked-file list and Git diff for credentials, real SMS, phone numbers, account identifiers, device identifiers, screenshots, and absolute paths.
- Confirm `build/`, `.keys/`, APKs, keystores, logs, local SDK files, and IDE state remain ignored and untracked.
- Choose an application ID you control before store distribution.
- Create and securely back up a dedicated release signing key. GitHub Actions intentionally produces debug-signed APKs only.
- Enable private vulnerability reporting and review `SECURITY.md`, `PRIVACY.md`, and every requested Android permission.
- Test fresh install, SMS receipt, Bark delivery, Wi-Fi loss, reboot, inbox reconciliation, retries, and battery restrictions with non-production data.
- Publish APKs only after independently verifying the signer and SHA-256 digest.
- Confirm applicable law, carrier terms, user consent, push-provider policy, and recipient security.
