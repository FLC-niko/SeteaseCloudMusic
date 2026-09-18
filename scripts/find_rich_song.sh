#!/bin/bash
# 自动切歌直到出现"歌词丰富"的曲目，然后截图。
# 用法: ./find_rich_song.sh [max_skip] [out.png]
set -e
MAX=${1:-8}
OUT=${2:-/tmp/rich.png}
PKG=com.luna.music

for i in $(seq 1 "$MAX"); do
  adb shell screencap -p /sdcard/_probe.png >/dev/null 2>&1
  adb pull /sdcard/_probe.png /tmp/_probe.png >/dev/null 2>&1
  # 统计亮文字行块数量
  rows=$(python3 "$(dirname "$0")/lyric_probe.py" /tmp/_probe.png 2>/dev/null \
        | sed -n '/纯白文字/,/含暗淡文字/p' | grep -c "center=" || true)
  echo "try $i: bright text rows = $rows"
  if [ "$rows" -ge 4 ]; then
    cp /tmp/_probe.png "$OUT"
    echo "found rich lyrics -> $OUT"
    exit 0
  fi
  adb shell input tap 683 2124   # 下一首
  sleep 6
done
echo "no rich song found in $MAX tries"
