#!/usr/bin/env bash
# Salin hasil build SPA (home/dist-mobile) ke folder www app native (Capacitor).
# Lokasi dideteksi otomatis, jadi jalan di mesin mana pun (frappe-bench,
# frappe-bench16, dll) tanpa perlu mengubah path di sini.
#
# Pakai:  bash _sync_www.sh
# Override manual (kalau perlu):  DIST_MOBILE=/path/ke/dist-mobile bash _sync_www.sh
set -e

# DST = folder www DI DALAM repo native ini → diambil relatif ke lokasi script,
# jadi selalu benar di mana pun repo ini berada.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DST="$SCRIPT_DIR/www"

# SRC = folder dist-mobile hasil `VITE_MOBILE=1 yarn build`.
# Deteksi otomatis di ~/frappe-bench*/apps/juragan/home/dist-mobile; bisa
# ditimpa lewat env DIST_MOBILE.
SRC="${DIST_MOBILE:-}"
if [ -z "$SRC" ]; then
	for c in "$HOME"/frappe-bench*/apps/juragan/home/dist-mobile; do
		[ -d "$c" ] && SRC="$c" && break
	done
fi

echo "SRC=$SRC"
echo "DST=$DST"

if [ -z "$SRC" ] || [ ! -d "$SRC" ]; then
	echo "ERROR: folder dist-mobile tidak ditemukan."
	echo "Build dulu:  cd <lokasi juragan>/home && VITE_MOBILE=1 yarn build"
	echo "Atau tentukan manual:  DIST_MOBILE=/path/ke/dist-mobile bash $0"
	exit 1
fi

rm -rf "$DST"
mkdir -p "$DST"
cp -r "$SRC"/. "$DST"/

echo "=== isi www ==="
ls "$DST"
echo "=== entry ==="
grep -oE 'src="[^"]+index-[^"]+\.js"' "$DST/index.html" | head -1
