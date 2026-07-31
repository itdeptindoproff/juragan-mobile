#!/usr/bin/env bash
set -e
SRC="$HOME/frappe-bench/apps/juragan/home/dist-mobile"
DST="$HOME/frappe-bench/apps/juragan-mobile/www"
echo "SRC=$SRC"
echo "DST=$DST"
[ -d "$SRC" ] || { echo "dist-mobile tidak ada, build dulu"; exit 1; }
rm -rf "$DST"
mkdir -p "$DST"
cp -r "$SRC"/. "$DST"/
echo "=== isi www ==="
ls "$DST"
echo "=== entry script ==="
grep -oE 'src="[^"]+index-[^"]+\.js"' "$DST/index.html"
