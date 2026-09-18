(() => {
  const player = document.querySelector('.amll-lyric-player');
  const out = { found: false };

  const maskedSpans = [...player.querySelectorAll('span')].filter((s) => {
    const cs = getComputedStyle(s);
    const m = cs.maskImage || cs.webkitMaskImage;
    return m && m !== 'none';
  });
  out.maskedCount = maskedSpans.length;
  if (!maskedSpans.length) return out;

  // 激活行 = 含 masked 词的行
  let line = maskedSpans[0];
  while (line && !/lyricLine/.test(line.className)) line = line.parentElement;
  if (!line) return out;
  out.found = true;

  const lr = line.getBoundingClientRect();
  const lcs = getComputedStyle(line);
  const main = line.querySelector('[class*="lyricMainLine"]');
  out.line = {
    text: (main ? main.textContent : '').slice(0, 40),
    rect: { x: +lr.x.toFixed(1), y: +lr.y.toFixed(1), w: +lr.width.toFixed(1), h: +lr.height.toFixed(1) },
    padding: lcs.padding,
    opacity: lcs.opacity,
    filter: lcs.filter,
    transform: lcs.transform,
    bright: lcs.getPropertyValue('--bright-mask-alpha').trim(),
    dark: lcs.getPropertyValue('--dark-mask-alpha').trim(),
    maskDur: lcs.getPropertyValue('--mask-alpha-duration').trim(),
  };

  // 行内每个词的完整 mask 信息
  const words = maskedSpans.slice(0, 8).map((s) => {
    const cs = getComputedStyle(s);
    const r = s.getBoundingClientRect();
    return {
      text: (s.textContent || '').slice(0, 6),
      rect: { x: +r.x.toFixed(1), w: +r.width.toFixed(1), h: +r.height.toFixed(1) },
      clientW: s.clientWidth,
      offsetW: s.offsetWidth,
      padding: cs.padding,
      margin: cs.margin,
      display: cs.display,
      maskImage: (cs.maskImage || cs.webkitMaskImage || '').slice(0, 300),
      maskSize: cs.maskSize || cs.webkitMaskSize,
      maskPosition: cs.maskPosition || cs.webkitMaskPosition,
      maskRepeat: cs.maskRepeat || cs.webkitMaskRepeat,
      color: cs.color,
      opacity: cs.opacity,
      textShadow: cs.textShadow,
      filter: cs.filter,
      transform: cs.transform,
    };
  });
  out.words = words;

  // 词内的字符级 span（强调/缩放用）
  const inner = maskedSpans[0].querySelector('span');
  if (inner) {
    const ics = getComputedStyle(inner);
    out.innerChar = {
      text: (inner.textContent || '').slice(0, 4),
      transform: ics.transform,
      textShadow: ics.textShadow,
      filter: ics.filter,
    };
  }

  // 带翻译的行（取一个 sub 非空的）
  const subs = [...player.querySelectorAll('[class*="lyricSubLine"]')]
    .filter((e) => (e.textContent || '').trim().length > 0);
  out.subSample = subs.slice(0, 3).map((e) => {
    const cs = getComputedStyle(e);
    const r = e.getBoundingClientRect();
    return {
      text: (e.textContent || '').slice(0, 20),
      fontSize: cs.fontSize,
      opacity: cs.opacity,
      lineHeight: cs.lineHeight,
      margin: cs.margin,
      padding: cs.padding,
      display: cs.display,
      rect: { y: +r.y.toFixed(1), h: +r.height.toFixed(1) },
    };
  });
  return out;
})()
