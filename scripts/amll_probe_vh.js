(() => {
  const mk = (css) => {
    const d = document.createElement('div');
    d.style.cssText = 'position:absolute;top:0;left:0;' + css;
    document.body.appendChild(d);
    const cs = getComputedStyle(d);
    const r = {
      height: cs.height,
      width: cs.width,
      fontSize: cs.fontSize,
    };
    d.remove();
    return r;
  };
  return {
    vh: mk('height:10vh;'),
    vw: mk('width:10vw;'),
    px: mk('height:100px;'),
    vmin: mk('height:10vmin;'),
    vmax: mk('height:10vmax;'),
    clampVh: mk('font-size:clamp(30px,4.2vh,42px);'),
    clampVw: mk('font-size:clamp(10px,4.2vw,60px);'),
    clampFixed: mk('font-size:clamp(30px,40px,42px);'),
    innerH: window.innerHeight,
    innerW: window.innerWidth,
    dpr: window.devicePixelRatio,
    rootFontSize: getComputedStyle(document.documentElement).fontSize,
    bodyFontSize: getComputedStyle(document.body).fontSize,
    htmlStyleFontSize: document.documentElement.style.fontSize,
    visualViewportScale: window.visualViewport ? window.visualViewport.scale : null,
  };
})()
