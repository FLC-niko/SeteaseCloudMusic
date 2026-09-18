#!/usr/bin/env python3
"""分析歌词截图的文字尺寸与位置：把 PNG 转成灰度裸数据后扫描行亮度分布。

用法: python3 lyric_metrics.py <shot.png>
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


def row_profile(raw, x0=0, x1=W):
    """每行的亮像素统计：返回 [(row, bright_count, max_val)]"""
    out = []
    for y in range(H):
        base = y * W
        bright = 0
        mx = 0
        for x in range(x0, x1, 2):          # 隔列采样，够用且快
            v = raw[base + x]
            if v > mx:
                mx = v
            if v > 200:
                bright += 1
        out.append((y, bright, mx))
    return out


def find_text_rows(profile, thresh=14):
    """把连续的高亮行聚合为文本块"""
    blocks = []
    cur = None
    for y, bright, mx in profile:
        if bright >= thresh:
            if cur is None:
                cur = [y, y, 0]
            cur[1] = y
            cur[2] = max(cur[2], bright)
        else:
            if cur is not None and cur[1] - cur[0] > 4:
                blocks.append(tuple(cur))
            cur = None
    if cur is not None and cur[1] - cur[0] > 4:
        blocks.append(tuple(cur))
    return blocks


def main():
    path = sys.argv[1]
    raw = load_gray(path)
    prof = row_profile(raw)
    blocks = find_text_rows(prof)

    print(f"== {path} ==")
    print(f"{'top':>5} {'bot':>5} {'h':>4} {'bright':>7}  {'center%':>7}")
    prev = None
    for top, bot, bright in blocks:
        h = bot - top + 1
        c = (top + bot) / 2 / H * 100
        gap = "" if prev is None else f"  gap={top-prev}"
        # 只列出文字块（过滤掉 UI 图标等小碎块）
        if h >= 18:
            print(f"{top:>5} {bot:>5} {h:>4} {bright:>7}  {c:>6.1f}%{gap}")
        prev = bot

    # 全局：亮像素最多的区域（聚焦行）与顶部空白分布
    hots = sorted(prof, key=lambda t: -t[1])[:1]
    print(f"最亮行: y={hots[0][0]} bright={hots[0][1]} ({hots[0][0]/H*100:.1f}%)")


if __name__ == "__main__":
    main()
