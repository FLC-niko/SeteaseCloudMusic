(() => {
  const player = document.querySelector('.amll-lyric-player');
  if (!player) return { error: 'no player' };
  const pr = player.getBoundingClientRect();
  const lines = [...player.querySelectorAll('[class*="lyricLine"]')];
  const subLines = [...player.querySelectorAll('[class*="lyricSubLine"]')];
  const out = {
    playerRect: { y: +pr.y.toFixed(1), h: +pr.height.toFixed(1), w: +pr.width.toFixed(1) },
    playerTransform: getComputedStyle(player).transform,
    lineCount: lines.length,
    subCount: subLines.length,
    hasBottomLine: !!player.querySelector('[class*="bottomLine"]'),
    playerClasses: player.className,
    emptyText: (player.textContent || '').slice(0, 60),
    lines: lines.slice(0, 12).map((el) => {
      const r = el.getBoundingClientRect();
      const cs = getComputedStyle(el);
      return {
        cls: el.className.replace(/FmKaba_/g, ''),
        y: Math.round(r.y),
        h: Math.round(r.height),
        opacity: cs.opacity,
        filter: cs.filter,
        transform: cs.transform,
        visibility: cs.visibility,
        text: (el.textContent || '').slice(0, 22),
      };
    }),
  };
  // 全局 atom 状态（从 React 根拿不到，改看 CSS 变量与时间）
  out.playerTimeVar = getComputedStyle(player).getPropertyValue('--amll-player-time').trim();
  out.bgEl = !!document.querySelector('[class*="background"]');
  return out;
})()
