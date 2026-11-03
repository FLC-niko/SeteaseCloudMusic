import React, { useEffect, useRef, useState } from "react";
import { LyricPlayer } from "@applemusic-like-lyrics/react";
import "@applemusic-like-lyrics/core/style.css";
import { parseLrc, parseYrc } from "@applemusic-like-lyrics/lyric";
import { parseTTML } from "@applemusic-like-lyrics/ttml";
import { createBridge, postToNative } from "./bridge";
import { resolvePlayerLayout, playbackPosition, formatTime } from "./adaptive";
import "./player.css";

const emptyTrack = { title: "未在播放", artist: "从音乐库中挑选喜欢的音乐", coverUrl: "", id: null };
const paths = {
  play: "M8 5v14l11-7z", pause: "M6 5h4v14H6zm8 0h4v14h-4z",
  previous: "M6 5h2v14H6zm3 7 11 7V5z", next: "M16 5h2v14h-2zM4 5v14l11-7z",
  queue: "M4 5h16v2H4zm0 5h12v2H4zm0 5h8v2H4zm12-1v7l6-3.5z",
  mode: "M7 4 3 8l4 4V9h10v3l4-4-4-4v3H7zm10 8v3H7v-3l-4 4 4 4v-3h10v3l4-4z",
};
function Glyph({ name }) { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d={paths[name]} /></svg>; }
function parseLyrics(raw = "") {
  if (!raw.trim()) return [];
  try {
    if (raw.includes("<tt") || raw.includes("<tt:")) return parseTTML(raw).lines;
    return /^\[\d+,\d+\]/m.test(raw) ? parseYrc(raw) : parseLrc(raw);
  } catch { return []; }
}

export default function PlayerApp() {
  const [track, setTrack] = useState(emptyTrack);
  const [lyrics, setLyrics] = useState([]);
  const [position, setPosition] = useState(0);
  const [playback, setPlayback] = useState({ playing: false, durationMs: 0 });
  const [layout, setLayout] = useState({ width: window.innerWidth, height: window.innerHeight, fold: null });
  const [showLyrics, setShowLyrics] = useState(false);
  const [seeking, setSeeking] = useState(null);
  const sync = useRef({ position: 0, duration: 0, playing: false, at: performance.now() });
  const mode = resolvePlayerLayout(layout.width, layout.height, layout.fold);

  useEffect(() => {
    const resize = () => setLayout(value => ({ ...value, width: window.innerWidth, height: window.innerHeight }));
    window.addEventListener("resize", resize);
    const dispose = createBridge(message => {
      const payload = message?.payload ?? {};
      if (message.type === "SET_TRACK") {
        setTrack({ ...emptyTrack, ...payload });
        setLyrics(parseLyrics(payload.lrc));
      } else if (message.type === "SET_PLAYBACK") {
        sync.current = { position: payload.currentTimeMs ?? 0, duration: payload.durationMs ?? 0, playing: !!payload.playing, at: performance.now() };
        setPlayback(payload);
        setPosition(sync.current.position);
      } else if (message.type === "SET_LAYOUT") {
        setLayout({ width: payload.width, height: payload.height, fold: payload.fold ?? null });
      }
    });
    postToNative({ type: "WEB_READY" });
    return () => { dispose(); window.removeEventListener("resize", resize); };
  }, []);

  useEffect(() => {
    if (!playback.playing) return;
    let frame;
    const tick = () => { setPosition(playbackPosition(sync.current, performance.now())); frame = requestAnimationFrame(tick); };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [playback.playing]);

  const seek = timeMs => {
    sync.current = { ...sync.current, position: timeMs, at: performance.now() };
    setPosition(timeMs);
    postToNative({ type: "SEEK_TO", payload: { timeMs } });
  };
  const controls = <div className="controls">
    <input className="seek" aria-label="播放进度" type="range" min="0" max={Math.max(1, playback.durationMs ?? 0)}
      value={seeking ?? position} disabled={!track.id || !playback.durationMs}
      onChange={event => setSeeking(Number(event.target.value))}
      onPointerCancel={() => setSeeking(null)}
      onPointerUp={event => { seek(Number(event.currentTarget.value)); setSeeking(null); }}
      onKeyUp={event => { if (["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) { seek(Number(event.currentTarget.value)); setSeeking(null); } }} />
    <div className="times"><span>{formatTime(seeking ?? position)}</span><span>-{formatTime(Math.max(0, (playback.durationMs ?? 0) - (seeking ?? position)))}</span></div>
    <div className="transport">
      <button aria-label="切换播放模式" disabled={!track.id} onClick={() => postToNative({ type: "TOGGLE_MODE" })}><Glyph name="mode" /></button>
      <button aria-label="上一首" disabled={!track.id} onClick={() => postToNative({ type: "PREV_TRACK" })}><Glyph name="previous" /></button>
      <button className="play" aria-label={playback.playing ? "暂停" : "播放"} disabled={!track.id} onClick={() => postToNative({ type: "TOGGLE_PLAY" })}><Glyph name={playback.playing ? "pause" : "play"} /></button>
      <button aria-label="下一首" disabled={!track.id} onClick={() => postToNative({ type: "NEXT_TRACK" })}><Glyph name="next" /></button>
      <button aria-label="播放队列" onClick={() => postToNative({ type: "SHOW_QUEUE" })}><Glyph name="queue" /></button>
    </div>
  </div>;
  const paneStyle = side => {
    const fold = layout.fold;
    if (mode === "book") return side === "primary"
      ? { left: 0, top: 0, width: Math.max(0, fold.left - 12), height: "100%" }
      : { left: fold.right + 12, top: 0, right: 0, height: "100%" };
    if (mode === "tabletop") return side === "primary"
      ? { left: 0, top: 0, width: "100%", height: Math.max(0, fold.top - 12) }
      : { left: 0, top: fold.bottom + 12, width: "100%", bottom: 0 };
    return undefined;
  };
  return <main className={`player ${mode} ${showLyrics ? "show-lyrics" : ""}`} data-layout={mode}>
    <div className="player-background" style={{ backgroundImage: track.coverUrl ? `url(${JSON.stringify(track.coverUrl)})` : undefined }} />
    <div className="player-content">
      <section className="album-pane" style={paneStyle("primary")} aria-label="封面与播放控制">
        <p className="eyebrow">NOW PLAYING · 正在播放</p>
        <div className="artwork-space">{track.coverUrl ? <img className="artwork" src={track.coverUrl} alt="专辑封面" /> : <div className="artwork artwork-placeholder">♫</div>}</div>
        <div className="track-caption"><h1>{track.title}</h1><p>{track.artist}</p></div>
        {mode !== "tabletop" && controls}
        <button className="mobile-toggle" onClick={() => setShowLyrics(true)}>查看歌词</button>
      </section>
      <section className="lyrics-pane" style={paneStyle("secondary")} aria-label="歌词">
        <p className="lyrics-heading">LYRICS · 歌词</p>
        {mode === "tabletop" && controls}
        {lyrics.length > 0 ? <LyricPlayer className="lyrics-viewport" lyricLines={lyrics} currentTime={Math.round(position)} playing={!!playback.playing}
          alignAnchor="center" alignPosition={0.35} enableSpring enableScale enableBlur
          onLyricLineClick={event => seek(event.line.getLine().startTime)} /> : <div className="empty-lyrics">{track.id ? "暂无歌词" : "选择一首音乐，开始聆听"}</div>}
        {mode === "compact" && controls}
        <button className="mobile-toggle" onClick={() => setShowLyrics(false)}>返回封面</button>
      </section>
    </div>
  </main>;
}
