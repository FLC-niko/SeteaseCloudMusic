(() => {
  const player = document.querySelector('.amll-lyric-player');
  const active = player.querySelector('[class*="lyricLine"].active, .active[class*="lyricLine"]')
    || [...player.querySelectorAll('[class*="lyricLine"]')].find((e) => /active/.test(e.className));
  if (!active) return { error: 'no active line' };
  const out = {
    cls: active.className,
    html: active.outerHTML.slice(0, 3000),
    // 该行内所有后代的样式摘要
    descendants: [...active.querySelectorAll('*')].slice(0, 20).map((el) => {
      const cs = getComputedStyle(el);
      const r = el.getBoundingClientRect();
      return {
        tag: el.tagName,
        cls: String(el.className).replace(/FmKaba_/g, '').slice(0, 40),
        box: [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height)],
        mask: ((cs.maskImage || cs.webkitMaskImage || '').slice(0, 120)),
        maskSize: cs.maskSize || cs.webkitMaskSize,
        maskPos: cs.maskPosition || cs.webkitMaskPosition,
        bg: cs.backgroundImage.slice(0, 60),
        bgClip: cs.webkitBackgroundClip || cs.backgroundClip,
        color: cs.color,
        filter: cs.filter,
        opacity: cs.opacity,
        transform: cs.transform,
      };
    }),
  };
  // 页面里是否存在任何 mask
  out.anyMaskInPage = [...player.querySelectorAll('*')].some((el) => {
    const cs = getComputedStyle(el);
    const m = cs.maskImage || cs.webkitMaskImage;
    return m && m !== 'none';
  });
  // CSS 变量
  const acs = getComputedStyle(active);
  out.vars = {
    bright: acs.getPropertyValue('--bright-mask-alpha').trim(),
    dark: acs.getPropertyValue('--dark-mask-alpha').trim(),
    maskDur: acs.getPropertyValue('--mask-alpha-duration').trim(),
  };
  return out;
})()
