(() => {
  const player = document.querySelector('.amll-lyric-player');
  const out = {};

  const walk = (el, label) => {
    const chain = [];
    let cur = el;
    for (let i = 0; i < 6 && cur && cur !== document.body; i++) {
      const cs = getComputedStyle(cur);
      chain.push({
        cls: String(cur.className).replace(/FmKaba_/g, '').slice(0, 34),
        opacity: cs.opacity,
        filter: cs.filter === 'none' ? undefined : cs.filter,
        color: cs.color,
        bright: cs.getPropertyValue('--bright-mask-alpha').trim() || undefined,
        dark: cs.getPropertyValue('--dark-mask-alpha').trim() || undefined,
      });
      cur = cur.parentElement;
    }
    out[label] = chain;
  };

  const lines = [...player.querySelectorAll('[class*="lyricLine"]')];
  const active = lines.find((e) => /active/.test(e.className));
  const plain = lines.find((e) => !/active/.test(e.className) && e.querySelector('span'));

  if (active) {
    const w = active.querySelector('[class*="lyricMainLine"] span span') || active.querySelector('span');
    if (w) walk(w, 'activeChain');
  }
  if (plain) {
    const w = plain.querySelector('[class*="lyricMainLine"] span span') || plain.querySelector('span');
    if (w) walk(w, 'plainChain');
    out.plainText = (plain.textContent || '').slice(0, 24);
  }

  // 带翻译的行：找 sub 非空
  const withSub = lines.filter((e) => {
    const subs = [...e.querySelectorAll('[class*="lyricSubLine"]')];
    return subs.some((s) => (s.textContent || '').trim().length > 0);
  });
  out.subLines = withSub.slice(0, 3).map((e) => {
    const subs = [...e.querySelectorAll('[class*="lyricSubLine"]')]
      .filter((s) => (s.textContent || '').trim().length > 0);
    const s = subs[0];
    const cs = getComputedStyle(s);
    const r = s.getBoundingClientRect();
    const er = e.getBoundingClientRect();
    return {
      mainText: (e.querySelector('[class*="lyricMainLine"]') || {}).textContent?.slice(0, 20),
      subText: (s.textContent || '').slice(0, 20),
      subFontSize: cs.fontSize,
      subLineHeight: cs.lineHeight,
      subOpacity: cs.opacity,
      subMargin: cs.margin,
      subBoxH: +r.height.toFixed(1),
      subOffsetFromLineTop: +(r.y - er.y).toFixed(1),
      lineH: +er.height.toFixed(1),
    };
  });

  // 词与词之间的间距（判断是否有额外空白）
  if (active) {
    const main = active.querySelector('[class*="lyricMainLine"]');
    const spans = [...main.querySelectorAll(':scope > span')];
    out.activeWordBoxes = spans.slice(0, 6).map((s) => {
      const r = s.getBoundingClientRect();
      const inner = s.querySelector('span');
      const ir = inner ? inner.getBoundingClientRect() : null;
      return {
        text: (s.textContent || '').slice(0, 6),
        outerX: +r.x.toFixed(1), outerW: +r.width.toFixed(1),
        innerX: ir ? +ir.x.toFixed(1) : null, innerW: ir ? +ir.width.toFixed(1) : null,
        h: +r.height.toFixed(1),
      };
    });
    out.activeMainStyle = (() => {
      const cs = getComputedStyle(main);
      return {
        padding: cs.padding, margin: cs.margin, lineHeight: cs.lineHeight,
        fontSize: cs.fontSize, letterSpacing: cs.letterSpacing,
        wordSpacing: cs.wordSpacing, textAlign: cs.textAlign,
        width: cs.width, opacity: cs.opacity,
      };
    })();
  }
  return out;
})()
