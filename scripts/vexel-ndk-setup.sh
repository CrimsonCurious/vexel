#!/system/bin/sh

set -e

NDK_ARCHIVE="vexel-ndk-arm64.7z"
VEXEL_HOME="$HOME/.vexel"

echo "[VEXEL-NDK] Installing NDK plugin..."

if [ ! -f "$NDK_ARCHIVE" ]; then
    echo "[ERROR] $NDK_ARCHIVE not found"
    exit 1
fi

if [ ! -d "$VEXEL_HOME" ]; then
    echo "[ERROR] Vexel is not installed"
    echo "Install Vexel first."
    exit 1
fi

echo "[VEXEL-NDK] Extracting..."
7z x "$NDK_ARCHIVE" -o"$VEXEL_HOME/"

echo "[VEXEL-NDK] Installation complete!"
echo "[VEXEL-NDK] Location: $VEXEL_HOME/ndk-arm64"