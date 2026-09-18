#!/usr/bin/env python3
"""打印截图某个小区域的灰度网格，用于判断笔画核心亮度（区分「整体降透明」与「仅模糊」）。

用法: python3 pixel_grid.py <shot.png> <x> <y> <w> <h> [step]
"""
import subprocess
import sys

W, H = 1080, 2460


def load_rgb(path):
    return subprocess.run(
        ["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
        capture_output=True, check=True).stdout


def main():
    path, x, y, w, h = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5])
    step = int(sys.argv[6]) if len(sys.argv) > 6 else 4
    raw = load_rgb(path)
    print(f"== {path} @({x},{y}) {w}x{h} step={step} ==")
    for yy in range(y, y + h, step):
        row = []
        for xx in range(x, x + w, step):
            i = (yy * W + xx) * 3
            r, g, b = raw[i], raw[i + 1], raw[i + 2]
            lum = int(0.299 * r + 0.587 * g + 0.114 * b)
            row.append(f"{lum:>3}")
        print(" ".join(row))


if __name__ == "__main__":
    main()
