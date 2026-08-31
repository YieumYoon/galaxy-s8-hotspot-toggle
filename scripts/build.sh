#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$ROOT_DIR/app/src/main"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" || ! -d "$SDK_ROOT" ]]; then
    echo "Set ANDROID_SDK_ROOT or ANDROID_HOME to an installed Android SDK." >&2
    exit 1
fi

ANDROID_JAR="$(find "$SDK_ROOT/platforms" -maxdepth 2 -name android.jar -print | sort -V | tail -n 1)"
AAPT2="$(find "$SDK_ROOT/build-tools" -maxdepth 2 -type f -name aapt2 -print | sort -V | tail -n 1)"

if [[ -z "$ANDROID_JAR" || -z "$AAPT2" ]]; then
    echo "Install an Android SDK platform and Android SDK Build-Tools." >&2
    exit 1
fi

BUILD_TOOLS_DIR="$(dirname "$AAPT2")"
D8="$BUILD_TOOLS_DIR/d8"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"

for tool in "$D8" "$ZIPALIGN" "$APKSIGNER"; do
    if [[ ! -x "$tool" ]]; then
        echo "Required Android build tool not found: $tool" >&2
        exit 1
    fi
done

if [[ -d "$BUILD_DIR" ]]; then
    rm -rf "$BUILD_DIR"
fi
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$DIST_DIR"

COMPILED_RES="$BUILD_DIR/resources.zip"
UNSIGNED_APK="$BUILD_DIR/app-unsigned.apk"
ALIGNED_APK="$BUILD_DIR/app-aligned.apk"
OUTPUT_APK="$DIST_DIR/galaxy-s8-hotspot-toggle.apk"

"$AAPT2" compile --dir "$APP_DIR/res" -o "$COMPILED_RES"
"$AAPT2" link \
    -I "$ANDROID_JAR" \
    --manifest "$APP_DIR/AndroidManifest.xml" \
    --min-sdk-version 26 \
    --target-sdk-version 28 \
    "$COMPILED_RES" \
    -o "$UNSIGNED_APK"

java_sources=()
while IFS= read -r -d '' source_file; do
    java_sources+=("$source_file")
done < <(find "$APP_DIR/java" -type f -name '*.java' -print0)

if [[ ${#java_sources[@]} -eq 0 ]]; then
    echo "No Java sources found." >&2
    exit 1
fi

javac \
    -source 8 \
    -target 8 \
    -bootclasspath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    "${java_sources[@]}"

class_files=()
while IFS= read -r -d '' class_file; do
    class_files+=("$class_file")
done < <(find "$BUILD_DIR/classes" -type f -name '*.class' -print0)

"$D8" \
    --lib "$ANDROID_JAR" \
    --min-api 26 \
    --output "$BUILD_DIR/dex" \
    "${class_files[@]}"

zip -q -j "$UNSIGNED_APK" "$BUILD_DIR/dex/classes.dex"
"$ZIPALIGN" -f 4 "$UNSIGNED_APK" "$ALIGNED_APK"

if [[ -n "${APK_KEYSTORE:-}" ]]; then
    : "${APK_KEY_ALIAS:?Set APK_KEY_ALIAS when APK_KEYSTORE is set}"
    : "${APK_KEYSTORE_PASSWORD:?Set APK_KEYSTORE_PASSWORD when APK_KEYSTORE is set}"
    : "${APK_KEY_PASSWORD:?Set APK_KEY_PASSWORD when APK_KEYSTORE is set}"
    KEYSTORE="$APK_KEYSTORE"
    KEY_ALIAS="$APK_KEY_ALIAS"
    STORE_PASSWORD_SPEC="env:APK_KEYSTORE_PASSWORD"
    KEY_PASSWORD_SPEC="env:APK_KEY_PASSWORD"
else
    KEYSTORE="$BUILD_DIR/debug.keystore"
    KEY_ALIAS="androiddebugkey"
    STORE_PASSWORD="android"
    KEY_PASSWORD="android"
    STORE_PASSWORD_SPEC="pass:$STORE_PASSWORD"
    KEY_PASSWORD_SPEC="pass:$KEY_PASSWORD"
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -alias "$KEY_ALIAS" \
        -dname "CN=Android Debug,O=Android,C=US" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        >/dev/null 2>&1
fi

"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias "$KEY_ALIAS" \
    --ks-pass "$STORE_PASSWORD_SPEC" \
    --key-pass "$KEY_PASSWORD_SPEC" \
    --out "$OUTPUT_APK" \
    "$ALIGNED_APK"

"$APKSIGNER" verify --verbose "$OUTPUT_APK"

if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$OUTPUT_APK"
else
    shasum -a 256 "$OUTPUT_APK"
fi

echo "Built $OUTPUT_APK"
