#!/system/bin/sh

set -e

VEXEL_ARCHIVE="vexel-base.7z"
VEXEL_HOME="$HOME/.vexel"
BIN_DIR="$(dirname "$HOME")/usr/bin"

echo "[VEXEL] Extracting..."

if [ ! -f "$VEXEL_ARCHIVE" ]; then
    echo "[ERROR] $VEXEL_ARCHIVE not found"
    exit 1
fi

7z x "$VEXEL_ARCHIVE" -o"$HOME"

echo "[VEXEL] Installing launcher..."

if [ ! -f "$HOME/vexel" ]; then
    echo "[ERROR] vexel launcher not found in archive"
    exit 1
fi

chmod +x "$HOME/vexel"
chmod +x "$VEXEL_HOME/runtime/bin/python3.14"

mkdir -p "$BIN_DIR"
mv -f "$HOME/vexel" "$BIN_DIR/vexel"

echo "[VEXEL] Installed successfully!"
echo "[VEXEL] Home: $VEXEL_HOME"