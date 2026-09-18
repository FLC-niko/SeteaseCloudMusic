#!/usr/bin/env python3
"""分析歌词截图：灰度裸数据 + 自适应阈值找文字行，并输出 ASCII 亮度剖面。

用法: python3 lyric_probe.py <shot.png> [--profile]
"""
import subprocess
import sys

W, H = 1080, 2460


def load_gray(path):
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        capture_output=True, check=True).stdout
    if len(raw) != W * H:
        raise SystemExit(f"unexpected size {len(raw)} != {W*H}")
    return raw


def row_stats(raw):
    """每行：纯白像素数(>200)与含暗字像素数(>130)"""
    stats = []
    for y in range(H):
        base = y * W
        hard = 0
        soft = 0
        mx = 0
        for x in range(0, W, 2):
            v = raw[base + x]
            if v > mx:
                mx = v
            if v > 200:
                hard += 2
            if v > 130:
                soft += 2
        stats.append((y, hard, soft, mx))
    return stats


def blocks(stats, key, min_bright, min_h=8):
    out = []
    cur = None
    for row in stats:
        y = row[0]
        b = row[key]
        if b >= min_bright:
            if cur is None:
                cur = [y, y]
            cur[1] = y
        else:
            if cur is not None and cur[1] - cur[0] >= min_h:
                out.append(tuple(cur))
            cur = None
    if cur is not None and cur[1] - cur[0] >= min_h:
        out.append(tuple(cur))
    return out


def report(path, show_profile=False):
    raw = load_gray(path)
    stats = row_stats(raw)
    print(f"== {path} ==")

    for key, name, th in ((1, "纯白文字 (gray>200)", 20), (2, "含暗淡文字 (gray>130)", 40)):
        bl = [b for b in blocks(stats, key, th) if b[1] - b[0] >= 10]
        print(f"\n-- {name} --")
        prev = None
        for top, bot in bl:
            h = bot - top + 1
            gap = "" if prev is None else f" gap={top - prev}"
            print(f"  y {top:>4}-{bot:<4} h={h:<4} center={((top+bot)/2/H*100):>5.1f}%{gap}")
            prev = bot

    if show_profile:
        print("\n-- 剖面（每 20 行）--")
        for y in range(0, H, 20):
            _, hard, soft, mx = stats[y]
            bar = "#" * min(60, soft // 12)
            print(f"{y:>5} {mx:>3} {bar}")


if __name__ == "__main__":
    report(sys.argv[1], "--profile" in sys.argv)
