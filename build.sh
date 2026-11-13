#!/data/data/com.termux/files/usr/bin/bash
# SPDX-License-Identifier: AGPL-3.0-only
# Build script for Alarm app
# Usage: ./build.sh [debug|release|clean]

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

export ANDROID_HOME="${ANDROID_HOME:-/data/data/com.termux/files/home/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

GRADLE="${GRADLE:-gradle}"

case "${1:-debug}" in
  debug)
    echo "==> Building debug APK..."
    "$GRADLE" assembleDebug
    ;;
  release)
    echo "==> Building release APK..."
    "$GRADLE" assembleRelease
    ;;
  clean)
    echo "==> Cleaning build..."
    "$GRADLE" clean
    ;;
  install)
    echo "==> Building and installing debug APK..."
    "$GRADLE" installDebug
    ;;
  *)
    echo "Usage: $0 [debug|release|clean|install]"
    exit 1
    ;;
esac

# Print the resulting APK path for debug builds
if [ -d "$SCRIPT_DIR/app/build/outputs/apk" ]; then
  echo ""
  echo "==> APKs:"
  find "$SCRIPT_DIR/app/build/outputs/apk" -name "*.apk" 2>/dev/null
fi

# Copy the built APK to the phone's Downloads folder so it can be installed from a file manager.
# Skipped for "clean". Override the destination with: DOWNLOAD_DIR=/some/dir ./build.sh
if [ "${1:-debug}" != "clean" ]; then
  VARIANT="${1:-debug}"
  [ "$VARIANT" = "install" ] && VARIANT="debug"
  APK="$(find "$SCRIPT_DIR/app/build/outputs/apk/$VARIANT" -name "*.apk" 2>/dev/null | head -n 1)"
  DEST=""
  for d in "${DOWNLOAD_DIR:-}" "$HOME/storage/downloads" "/sdcard/Download"; do
    if [ -n "$d" ] && [ -d "$d" ] && [ -w "$d" ]; then DEST="$d"; break; fi
  done
  if [ -n "$APK" ] && [ -n "$DEST" ]; then
    cp "$APK" "$DEST/crystal-alarm-$VARIANT.apk"
    echo ""
    echo "==> Copied to $DEST/crystal-alarm-$VARIANT.apk"
  elif [ -n "$APK" ]; then
    echo ""
    echo "==> No Downloads folder found. Run 'termux-setup-storage' once, then build again to get the APK copied."
  fi
fi