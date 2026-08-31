# Compatibility

Compatibility is determined by the installed `com.android.settings` package.
A matching phone model can stop working after an OS update, while another regional
variant of the same model may use different carrier customizations.

## Current status

| Confidence | Devices and firmware | Notes |
| --- | --- | --- |
| Verified | Galaxy S8 `SM-G950U`, Android 9, `G950USQU8DUJ1`, Verizon CSC | Start, state detection, and stop tested on hardware |
| High-priority candidate | Galaxy S8+ `SM-G955U` on Android 9 | Closely related US Snapdragon firmware; not tested |
| Candidate | Galaxy Note8 `SM-N950U` on Android 9 | Android 8 public code lacks the required off-to-on branch; Android 9 must be inspected |
| Candidate | `SM-G950U1`, `SM-G955U1`, and `SM-N950U1` on Android 9 | Factory-unlocked Settings and active CSC may differ |
| Research candidate | Galaxy S9/S9+ `SM-G960U`/`SM-G965U` and Note9 `SM-N960U` on Android 9 | Similar generation, but no exact code match has been verified |
| Unknown | Global Exynos and other Android 9 Samsung devices | Do not infer support from model or Android version alone |
| Not compatible with this exact start path | Publicly inspected older Galaxy S7 Android 7, early S8 Android 7, and Note8 Android 8 builds | They contain the action name but only restart an already enabled or enabling hotspot |
| Not applicable | Non-Samsung Android devices | The Samsung Settings components do not exist |

No currently supported modern Galaxy model has been confirmed compatible.

## Required firmware signature

The start path requires all of the following in the installed Samsung Settings app:

1. `com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver` exists and
   is externally reachable without a signature-only permission.
2. It receives `com.samsung.android.net.wifi.WIFI_AP_DRIVER_STATE_HANGED`.
3. It reads the `wifi_ap_error_code` extra.
4. A value of `14` reaches a call equivalent to `semSetWifiApEnabled(null, true)`
   even while the hotspot is off.

The stop path additionally requires the exported activity
`com.samsung.android.settings.wifi.WifiWarning`. Some firmware may display a
Samsung confirmation dialog on first use.

The app's preflight verifies component existence and export state only. Android's
package manager cannot prove that the receiver body contains the required value-14
branch, so a successful preflight is not a compatibility guarantee.

## Reporting a result safely

Open the compatibility-report issue form and include only:

- Public model number, such as `SM-G955U`
- Android version
- Firmware build number
- CSC or carrier, if known
- Whether preflight, start, state detection, and stop worked

Never include device serial numbers, IMEIs, phone numbers, subscriber identifiers,
MAC addresses, SSIDs, account details, full logs, or extracted Samsung APK files.
