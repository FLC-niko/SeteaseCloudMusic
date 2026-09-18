(() => {
  const mk = (css, tag = 'span') => {
    const d = document.createElement(tag);
    d.textContent = '测试Abc';
    d.style.cssText = 'position:absolute;left:-9999px;font-size:30px;' + css;
    document.body.appendChild(d);
    const cs = getComputedStyle(d);
    const res = {
      maskImage: (cs.maskImage || '').slice(0, 120),
      webkitMaskImage: (cs.webkitMaskImage || '').slice(0, 120),
      maskSize: cs.maskSize || cs.webkitMaskSize,
      maskPosition: cs.maskPosition || cs.webkitMaskPosition,
    };
    d.remove();
    return res;
  };
  return {
    supports: {
      maskImage: CSS.supports('mask-image', 'none'),
      webkitMaskImage: CSS.supports('-webkit-mask-image', 'none'),
      maskImageGradient: CSS.supports('mask-image', 'linear-gradient(to right,red,blue)'),
    },
    validGradient: mk('mask-image: linear-gradient(to right,rgba(0,0,0,1) 30%,rgba(0,0,0,0.4) 70%);'),
    nanGradient: mk('mask-image: linear-gradient(to right,rgba(0,0,0,1) NaN%,rgba(0,0,0,0.4) NaN%);'),
    validWebkit: mk('webkit-mask-image: linear-gradient(to right,rgba(0,0,0,1) 30%,rgba(0,0,0,0.4) 70%);'),
    playerSupportVar: (() => {
      const p = document.querySelector('.amll-lyric-player');
      return p ? p.className : null;
    })(),
  };
})()
