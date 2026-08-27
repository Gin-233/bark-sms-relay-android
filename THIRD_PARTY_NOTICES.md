# Third-party notices

This repository contains adapted assets and a test vector from third-party
open-source projects. These notices apply only to the items identified below;
the repository's original code and documentation remain under the root MIT
license.

## Google Material Icons

The following Android VectorDrawable files are adapted from Google Material
Icons:

- `res/drawable/ic_alert.xml` from `warning`
- `res/drawable/ic_arrow_back.xml` from `arrow_back`
- `res/drawable/ic_bell.xml` from `notifications`
- `res/drawable/ic_lock.xml` from `lock`
- `res/drawable/ic_power.xml` from `power_settings_new`

Upstream sources at commit `e083cc60`:

- [`warning`](https://github.com/google/material-design-icons/blob/e083cc60a0828fdd3b404cea0cb8a5b900e9c23e/src/alert/warning/materialicons/24px.svg)
- [`arrow_back`](https://github.com/google/material-design-icons/blob/e083cc60a0828fdd3b404cea0cb8a5b900e9c23e/src/navigation/arrow_back/materialicons/24px.svg)
- [`notifications`](https://github.com/google/material-design-icons/blob/e083cc60a0828fdd3b404cea0cb8a5b900e9c23e/src/social/notifications/materialicons/24px.svg)
- [`lock`](https://github.com/google/material-design-icons/blob/e083cc60a0828fdd3b404cea0cb8a5b900e9c23e/src/action/lock/materialicons/24px.svg)
- [`power_settings_new`](https://github.com/google/material-design-icons/blob/e083cc60a0828fdd3b404cea0cb8a5b900e9c23e/src/action/power_settings_new/materialicons/24px.svg)

License: Apache License 2.0. The complete license is included at
[`LICENSES/Apache-2.0.txt`](LICENSES/Apache-2.0.txt).

Changes: files were adapted for this project's Android VectorDrawable
resources and renamed; metadata, path syntax or geometry, keyhole details,
and colors were adjusted for this application's interface.

## Bark encryption test vector

`host-test/com/local/smsrelay/HostTests.java` includes the published encryption
test vector from [Bark's English encryption documentation at
`abbd3baa`](https://github.com/Finb/Bark/blob/abbd3baa342c90b0fa985c5b787b82b446a4628d/docs/en-us/encryption.md).

Bark is licensed under the MIT License, Copyright (c) 2018 Feng. Its complete
license notice is included at [`LICENSES/Bark-MIT.txt`](LICENSES/Bark-MIT.txt).

The Bark name is used only to identify protocol compatibility. This project is
not affiliated with or endorsed by the Bark project.
