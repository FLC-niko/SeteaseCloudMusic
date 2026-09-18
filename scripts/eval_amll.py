#!/usr/bin/env python3
"""通过 Chrome DevTools Protocol 读取 Android WebView 内 AMLL 歌词的真实计算样式。

用法: /tmp/wsvenv/bin/python eval_amll.py [js表达式文件]
默认执行内置的完整采样脚本，输出 JSON。
"""
import json
import sys
import urllib.request

import websocket


def get_ws_url():
    with urllib.request.urlopen("http://127.0.0.1:9222/json") as resp:
        pages = json.load(resp)
    for p in pages:
        if p.get("type") == "page":
            return p["webSocketDebuggerUrl"]
    raise SystemExit("no page found")


PROBE_JS = r"""
(() => {
  const out = {};
  out.viewport = {
    w: window.innerWidth, h: window.innerHeight,
    dpr: window.devicePixelRatio,
    vh: window.innerHeight / 100,          // 1vh 的像素值
  };
  const player = document.querySelector('.amll-lyric-player');
  if (!player) return { error: 'no lyric player' };
  const pr = player.getBoundingClientRect();
  out.playerBox = { x: pr.x, y: pr.y, w: pr.width, h: pr.height };
  const pcs = getComputedStyle(player);
  out.playerStyle = {
    fontSize: pcs.fontSize,
    fontWeight: pcs.fontWeight,
    fontFamily: pcs.fontFamily.slice(0, 80),
    color: pcs.color,
    cssVarFontSize: pcs.getPropertyValue('--amll-lp-font-size').trim(),
  };

  // 所有歌词行
  const lines = [...player.querySelectorAll('[class*="lyricLine"]')];
  out.lineCount = lines.length;
  out.lines = lines.slice(0, 14).map((el) => {
    const r = el.getBoundingClientRect();
    const cs = getComputedStyle(el);
    const mainEl = el.querySelector('[class*="lyricMainLine"]');
    const subEl = el.querySelector('[class*="lyricSubLine"]');
    const mcs = mainEl ? getComputedStyle(mainEl) : null;
    const words = mainEl ? [...mainEl.querySelectorAll('span')].slice(0, 4) : [];
    return {
      cls: el.className,
      box: { y: Math.round(r.y), h: Math.round(r.height) },
      opacity: cs.opacity,
      filter: cs.filter,
      transform: cs.transform,
      transformOrigin: cs.transformOrigin,
      padding: cs.padding,
      gap: cs.gap,
      main: mcs ? {
        fontSize: mcs.fontSize,
        lineHeight: mcs.lineHeight,
        fontWeight: mcs.fontWeight,
        letterSpacing: mcs.letterSpacing,
        padding: mcs.padding,
        margin: mcs.margin,
        text: (mainEl.textContent || '').slice(0, 30),
      } : null,
      sub: subEl ? {
        text: (subEl.textContent || '').slice(0, 30),
        fontSize: getComputedStyle(subEl).fontSize,
        opacity: getComputedStyle(subEl).opacity,
        margin: getComputedStyle(subEl).margin,
      } : null,
      wordSample: words.map((w) => {
        const wcs = getComputedStyle(w);
        const wr = w.getBoundingClientRect();
        return {
          text: (w.textContent || '').slice(0, 6),
          box: { x: Math.round(wr.x), w: Math.round(wr.width), h: Math.round(wr.height) },
          maskImage: (wcs.maskImage || wcs.webkitMaskImage || '').slice(0, 220),
          maskSize: wcs.maskSize || wcs.webkitMaskSize,
          maskPosition: wcs.maskPosition || wcs.webkitMaskPosition,
          maskRepeat: wcs.maskRepeat || wcs.webkitMaskRepeat,
          color: wcs.color,
          transform: wcs.transform,
          textShadow: wcs.textShadow,
          padding: wcs.padding,
          display: wcs.display,
        };
      }),
      lineVars: {
        bright: cs.getPropertyValue('--bright-mask-alpha').trim(),
        dark: cs.getPropertyValue('--dark-mask-alpha').trim(),
        bgScale: cs.getPropertyValue('--amll-lp-bg-line-scale').trim(),
      },
    };
  });
  return out;
})()
"""


def main():
    js = open(sys.argv[1]).read() if len(sys.argv) > 1 else PROBE_JS
    # Android WebView 的 DevTools 会拒绝带 Origin 的握手，必须抑制该头
    ws = websocket.create_connection(get_ws_url(), timeout=20, suppress_origin=True)
    ws.send(json.dumps({
        "id": 1,
        "method": "Runtime.evaluate",
        "params": {"expression": js, "returnByValue": True, "awaitPromise": True},
    }))
    while True:
        msg = json.loads(ws.recv())
        if msg.get("id") == 1:
            break
    ws.close()

    result = msg.get("result", {})
    if "exceptionDetails" in result:
        print("JS ERROR:", json.dumps(result["exceptionDetails"], ensure_ascii=False)[:800])
        return
    print(json.dumps(result.get("result", {}).get("value"), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
