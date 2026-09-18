(() => {
  const player = document.querySelector('.amll-lyric-player');
  const out = {};

  // 1) clamp/vh 解析验证
  const probe = document.createElement('div');
  probe.style.cssText = 'position:absolute;left:-9999px;height:4.2vh;font-size:clamp(30px,4.2vh,42px);';
  document.body.appendChild(probe);
  out.clampProbe = {
    height42vh: getComputedStyle(probe).height,
    fontSizeClamp: getComputedStyle(probe).fontSize,
    innerHeight: window.innerHeight,
    docClientHeight: document.documentElement.clientHeight,
    visualViewport: window.visualViewport ? window.visualViewport.height : null,
  };
  probe.remove();

  // 2) 找到带 mask 的词（真正的逐字高亮元素）
  const allSpans = [...player.querySelectorAll('span')];
  const masked = allSpans.filter((s) => {
    const cs = getComputedStyle(s);
    const m = cs.maskImage || cs.webkitMaskImage;
    return m && m !== 'none';
  });
  out.maskedCount = masked.length;

  // 3) 找出「激活行」：含 masked 词的祖先行
  const activeLine = masked.length ? masked[0].closest('[class*="lyricLine"]:not([class*="lyricMainLine"])') : null;
  out.activeFound = !!activeLine;

  const describeWord = (s) => {
    const cs = getComputedStyle(s);
    const r = s.getBoundingClientRect();
    return {
      text: (s.textContent || '').slice(0, 8),
      cls: s.className,
      box: { x: +r.x.toFixed(1), y: +r.y.toFixed(1), w: +r.width.toFixed(1), h: +r.height.toFixed(1) },
      padding: cs.padding,
      margin: cs.margin,
      display: cs.display,
      color: cs.color,
      opacity: cs.opacity,
      maskImage: (cs.maskImage || cs.webkitMaskImage || '').slice(0, 260),
      maskSize: cs.maskSize || cs.webkitMaskSize,
      maskRepeat: cs.maskRepeat || cs.webkitMaskRepeat,
      maskPosition: cs.maskPosition || cs.webkitMaskPosition,
      maskOrigin: cs.maskOrigin || cs.webkitMaskOrigin,
      textShadow: cs.textShadow,
      transform: cs.transform,
      filter: cs.filter,
    };
  };

  if (activeLine) {
    const lcs = getComputedStyle(activeLine);
    const r = activeLine.getBoundingClientRect();
    out.activeLine = {
      box: { y: +r.y.toFixed(1), h: +r.height.toFixed(1) },
      padding: lcs.padding,
      opacity: lcs.opacity,
      filter: lcs.filter,
      transform: lcs.transform,
      transformOrigin: lcs.transformOrigin,
      bright: lcs.getPropertyValue('--bright-mask-alpha').trim(),
      dark: lcs.getPropertyValue('--dark-mask-alpha').trim(),
      maskAlphaDur: lcs.getPropertyValue('--mask-alpha-duration').trim(),
      gradientMask: activeLine.classList.contains('gradientMask') ||
        [...activeLine.classList].some((c) => c.includes('gradientMask')),
    };
    out.activeWords = masked.filter((s) => activeLine.contains(s)).slice(0, 6).map(describeWord);
    // 该行内所有 span 的层级结构
    const main = activeLine.querySelector('[class*="lyricMainLine"]');
    if (main) {
      out.activeMainHTML = main.outerHTML.slice(0, 1400);
    }
  }

  // 4) 非激活行的普通词（对照）
  const anyLine = [...player.querySelectorAll('[class*="lyricLine"]:not([class*="lyricMainLine"])')]
    .find((el) => el !== activeLine && el.querySelector('span'));
  if (anyLine) {
    const cs = getComputedStyle(anyLine);
    out.plainLine = {
      opacity: cs.opacity,
      filter: cs.filter,
      bright: cs.getPropertyValue('--bright-mask-alpha').trim(),
      dark: cs.getPropertyValue('--dark-mask-alpha').trim(),
      transform: cs.transform,
      text: (anyLine.textContent || '').slice(0, 24),
    };
    const s = anyLine.querySelector('span');
    if (s) out.plainWord = describeWord(s);
    const sub = anyLine.querySelector('[class*="lyricSubLine"]');
    if (sub) {
      const scs = getComputedStyle(sub);
      out.plainSub = {
        text: (sub.textContent || '').slice(0, 24),
        fontSize: scs.fontSize,
        opacity: scs.opacity,
        display: scs.display,
        margin: scs.margin,
        lineHeight: scs.lineHeight,
      };
    }
  }

  // 5) 词的高度基准（fadeWidth 需要）
  out.wordFadeWidthCssVar = getComputedStyle(player).getPropertyValue('--amll-lp-word-fade-width').trim();
  out.playerFontSize = getComputedStyle(player).fontSize;
  return out;
})()
