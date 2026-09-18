#!/usr/bin/env python3
"""量化对比两张歌词截图的文字亮度与几何，用于对齐原生实现。

用法: python3 lyric_compare.py <shot.png> [y0 y1 ...]
"""
import subprocess
import sys

W, H = 1080, 2460


def load_rgb(path):
    return subprocess.run(
        ["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
        capture_output=True, check=True).stdout


def band_stats(raw, y0, y1, x0=0, x1=W):
    """统计某一横带内的亮度分布（背景 & 文字峰值）"""
    lum = []
    for y in range(y0, y1):
        base = y * W * 3
        for x in range(x0, x1, 2):
            i = base + x * 3
            r, g, b = raw[i], raw[i + 1], raw[i + 2]
            lum.append(0.299 * r + 0.587 * g + 0.114 * b)
    lum.sort()
    n = len(lum)
    return {
        "n": n,
        "p10": round(lum[n // 10], 1),
        "p50": round(lum[n // 2], 1),
        "p90": round(lum[n * 9 // 10], 1),
        "p99": round(lum[n * 99 // 100], 1),
        "max": round(lum[-1], 1),
    }


def main():
    path = sys.argv[1]
    raw = load_rgb(path)
    bands = []
    args = sys.argv[2:]
    for i in range(0, len(args), 2):
        bands.append((int(args[i]), int(args[i + 1])))

    print(f"== {path} ==")
    for y0, y1 in bands:
        s = band_stats(raw, y0, y1)
        print(f"y {y0:>4}-{y1:<4} p10={s['p10']:>6} p50={s['p50']:>6} "
              f"p90={s['p90']:>6} p99={s['p99']:>6} max={s['max']:>6}")


if __name__ == "__main__":
    main()
