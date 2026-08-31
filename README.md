# Galaxy S8 Hotspot Toggle

A small, permission-free Android utility for a firmware-specific hotspot path on
legacy Samsung Galaxy devices. It can start and stop Wi-Fi sharing on the one
firmware build that has been verified so far, without root access or a SIM card.

> [!IMPORTANT]
> This is not a universal tethering unlock. Compatibility depends on the exact
> Samsung Settings package installed on the phone, not just the model name.
> Use it only on devices you own or are authorized to test, and follow applicable
> carrier terms and local law.

## Verified device

| Device | Android | Firmware | CSC/carrier | Result |
| --- | --- | --- | --- | --- |
| Galaxy S8 `SM-G950U` | 9 | `G950USQU8DUJ1` | Verizon | Start, state detection, and stop verified |

Other variants are unverified. See [Compatibility](docs/compatibility.md) before
testing another phone.

## What it does

- Requests no Android permissions.
- Uses an explicit Samsung Settings broadcast to request hotspot startup.
- Observes the system hotspot-state broadcast and displays the current state.
- Uses an exported Samsung Settings warning activity to request hotspot shutdown.
- Falls back to checking the Samsung hotspot network interface when no sticky
  state broadcast is available.
- Performs a component-level compatibility preflight before enabling the button.

The app does **not** modify `tether_dun_required`, root the phone, patch firmware,
collect analytics, access the network, or contain Samsung firmware files.

## Build

Requirements:

- Android SDK with one installed platform and matching Build-Tools
- JDK 8 or newer
- `zip`, `rg`, and a POSIX shell environment

Build a locally debug-signed APK:

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
./scripts/privacy-check.sh
./scripts/build.sh
```

The APK is written to `dist/galaxy-s8-hotspot-toggle.apk`. The generated debug
keystore stays under the ignored `build/` directory.

For a consistently signed release, provide `APK_KEYSTORE`, `APK_KEY_ALIAS`,
`APK_KEYSTORE_PASSWORD`, and `APK_KEY_PASSWORD` to the build script. Never commit
the keystore or passwords.

## Install

Enable USB debugging on the phone, connect it with ADB, then run:

```bash
adb install -r dist/galaxy-s8-hotspot-toggle.apk
```

The public package name is `dev.legacyhotspot.s8`. It is intentionally neutral
and contains no developer name or personal identifier.

## Technical notes

The behavior and trust boundary are documented in [Implementation](docs/implementation.md).
The short version is that an exported component in the privileged Samsung Settings
app performs the actual hotspot operation. This app itself never receives the
privileged Wi-Fi permissions.

## Contributing

Compatibility reports are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first;
do not post IMEIs, serial numbers, phone numbers, MAC addresses, Wi-Fi names,
full system dumps, or proprietary APK files.

## License and trademarks

The project is licensed under the [Apache License 2.0](LICENSE). The launcher
symbol is adapted from Google's Apache-2.0-licensed Material `wifi_tethering`
icon; see [NOTICE](NOTICE).

Samsung, Galaxy, Verizon, Android, and Google are trademarks of their respective
owners. This independent project is not affiliated with or endorsed by them.
