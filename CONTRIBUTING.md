# Contributing

Bug fixes, build improvements, documentation corrections, and carefully scoped
compatibility reports are welcome.

## Compatibility reports

Use the compatibility-report issue form. Report only the public model, Android
version, firmware build, CSC/carrier if known, and the observed app behavior.

Do not upload or paste:

- IMEI, serial number, phone number, IMSI, ICCID, Android ID, or account data
- Wi-Fi SSIDs, BSSIDs, MAC addresses, IP addresses, or location data
- Complete `logcat`, bugreport, or `dumpsys` output
- Samsung APK, ODEX, VDEX, firmware image, or decompiled source archives
- Signing keys, access tokens, passwords, or private configuration

If a small log excerpt is essential, redact it locally and include only the lines
needed to explain the result.

## Pull requests

Before opening a pull request:

```bash
./scripts/privacy-check.sh
./scripts/build.sh
```

Keep the application permission-free unless a change has a clear, documented need.
Do not add analytics, telemetry, advertising, remote configuration, or background
network access.

Security problems should be reported privately as described in [SECURITY.md](SECURITY.md).
