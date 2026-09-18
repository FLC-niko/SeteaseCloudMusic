#!/bin/bash
# 在原生/AMLL Web 两种播放页风格间切换并截图，用于像素级对比。
# 用法: ./compare_lyric.sh <native|web> <out.png>
set -e
PKG=com.luna.music
ACT=$PKG/com.example.seteasecloudmusic.feature.main.MainActivity
MODE=$1
OUT=$2

case "$MODE" in
  web) STYLE=AMLL_WEB ;;
  *)   STYLE=NATIVE_COMPOSE ;;
esac

# 直接改 SharedPreferences（debug 包可用 run-as）
adb shell "run-as $PKG sh -c 'cat /data/data/$PKG/shared_prefs/app_settings_prefs.xml'" > /tmp/prefs.xml
python3 - "$STYLE" <<'PY'
import re, sys
style = sys.argv[1]
p = "/tmp/prefs.xml"
s = open(p, encoding="utf-8").read()
if 'name="player_style_mode"' in s:
    s = re.sub(r'<string name="player_style_mode">[^<]*</string>',
               f'<string name="player_style_mode">{style}</string>', s)
else:
    s = s.replace("</map>", f'    <string name="player_style_mode">{style}</string>\n</map>')
open(p, "w", encoding="utf-8").write(s)
PY

adb push /tmp/prefs.xml /data/local/tmp/app_settings_prefs.xml >/dev/null
adb shell "run-as $PKG cp /data/local/tmp/app_settings_prefs.xml /data/data/$PKG/shared_prefs/app_settings_prefs.xml"

# 重启应用并打开播放页
adb shell am force-stop $PKG
sleep 1
adb shell am start -n $ACT >/dev/null
sleep 4
# 点迷你播放条展开
adb shell input tap 540 2064
sleep 3

adb shell screencap -p /sdcard/_cmp.png
adb pull /sdcard/_cmp.png "$OUT" >/dev/null 2>&1
echo "saved $OUT ($STYLE)"
