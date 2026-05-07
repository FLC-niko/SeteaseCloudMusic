import React, { useEffect, useState } from "react";
import { useSetAtom, useAtomValue } from "jotai";
import * as AMLL from "@applemusic-like-lyrics/react-full";
import "@applemusic-like-lyrics/react-full/style.css";
import { parseLrc, parseYrc } from "@applemusic-like-lyrics/lyric";
import { postToNative, createBridge } from "./bridge";

const demoLrc = `[00:00.00]Setease Cloud Music\n[00:04.00]空间扭曲魔法 2.0 部署完毕！\n[00:08.00]按钮尺寸恢复，封面呈现！`;

// 绝美底层毛玻璃背景
const AppleMusicBackground = () => {
  const coverUrl = useAtomValue(AMLL.musicCoverAtom);
  return (
    <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", overflow: "hidden", zIndex: 0 }}>
      <div style={{
        position: 'absolute', top: '-20%', left: '-20%', width: '140%', height: '140%',
        backgroundImage: `url(${coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center',
        filter: 'blur(60px) saturate(180%)', transition: 'background-image 0.5s ease'
      }} />
      <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0, 0, 0, 0.35)' }} />
    </div>
  );
};

export default function PlayerApp() {
  const setLyricLines = useSetAtom(AMLL.musicLyricLinesAtom);
  const setCurrentTimeMs = useSetAtom(AMLL.musicPlayingPositionAtom);
  const setPlaying = useSetAtom(AMLL.musicPlayingAtom);
  const setMusicName = useSetAtom(AMLL.musicNameAtom);
  const setMusicArtists = useSetAtom(AMLL.musicArtistsAtom);
  const setMusicCover = useSetAtom(AMLL.musicCoverAtom);

  const setOnPlayOrResume = useSetAtom(AMLL.onPlayOrResumeAtom);
  const setOnRequestNextSong = useSetAtom(AMLL.onRequestNextSongAtom);
  const setOnRequestPrevSong = useSetAtom(AMLL.onRequestPrevSongAtom);
  const setOnSeekPosition = useSetAtom(AMLL.onSeekPositionAtom);

  const setStaticMode = useSetAtom(AMLL.lyricBackgroundStaticModeAtom);
  const setIsLyricPageOpened = useSetAtom(AMLL.isLyricPageOpenedAtom);
  const setShowBottomControl = useSetAtom(AMLL.showBottomControlAtom);
  const setPlayerControlsType = useSetAtom(AMLL.playerControlsTypeAtom);

  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    setStaticMode(true);
    setIsLyricPageOpened(false); // 默认收起歌词，展示大封面
    setShowBottomControl(true);
    setPlayerControlsType('controls');

    setOnPlayOrResume(() => () => postToNative({ type: "TOGGLE_PLAY" }));
    setOnRequestNextSong(() => () => postToNative({ type: "NEXT_TRACK" }));
    setOnRequestPrevSong(() => () => postToNative({ type: "PREV_TRACK" }));
    setOnSeekPosition(() => (timeMs) => postToNative({ type: "SEEK_TO", payload: { timeMs } }));

    setLyricLines(parseLrc(demoLrc));
    setMusicName("Setease Player");
    setMusicArtists(["Zhao Haixiang"]);
    setMusicCover("https://picsum.photos/500/500");

    const dispose = createBridge((msg) => {
      if (!msg?.type) return;
      switch (msg.type) {
        case "SET_PLAYBACK":
          setCurrentTimeMs((msg.payload?.currentTimeMs ?? 0) | 0);
          setPlaying(!!msg.payload?.playing);
          break;
        case "SET_TRACK":
          if (typeof msg.payload?.lrc === "string") {
            const raw = msg.payload.lrc.trim();
            if (raw) {
              // 检测是否为 YRC 格式（云音乐逐字歌词）
              if (raw.includes("(") && raw.includes(")") && raw.includes("[")) {
                try {
                  setLyricLines(parseYrc(raw));
                } catch(e) {
                  setLyricLines(parseLrc(raw));
                }
              } else {
                setLyricLines(parseLrc(raw));
              }
            } else {
              setLyricLines(parseLrc(demoLrc));
            }
          }
          if (msg.payload?.title) setMusicName(msg.payload.title);
          if (msg.payload?.artist) setMusicArtists([msg.payload.artist]);
          if (msg.payload?.coverUrl) setMusicCover(msg.payload.coverUrl);
          setCurrentTimeMs(0);
          break;
      }
    });

    postToNative({ type: "WEB_READY" });
    return () => dispose();
  }, []);

  return (
    <>
      <style>{`
              html, body, #root {
                width: 100vw;
                height: 100vh;
                margin: 0;
                padding: 0;
                overflow: hidden;
                background: #000;
                touch-action: none;
              }

              /* 屏蔽导致黑屏的原生背景 */
              ._8f42YG_background { display: none !important; opacity: 0 !important; }

              /* ========================================================= */
              /* 👑 空间扭曲魔法 3.0 (完美吸底 + 像素级比例调优) */
              /* ========================================================= */

              .-DaA0W_horizontalLayout {
                display: flex !important;
                flex-direction: column !important;
                align-items: center !important;
                justify-content: flex-start !important;
                width: 100vw !important;
                height: 100vh !important;
                padding: 6vh 6vw 4vh 6vw !important; /* 底部保留 4vh 安全距离 */
                box-sizing: border-box !important;
                gap: 2vh !important;
              }

              /* 1. 隐藏多余的横屏版拖拽条 */
              .-DaA0W_thumb { display: none !important; }

              /* 2. 封面基础设置 */
              .-DaA0W_cover {
                width: 85vw !important;
                max-width: 380px !important;
                height: auto !important;
                aspect-ratio: 1 !important;
                flex-shrink: 0 !important;
                position: relative !important;
                left: 0 !important;
                transform: none !important;
                transition: all 0.6s cubic-bezier(0.25, 1, 0.5, 1) !important;
                box-shadow: 0 20px 40px rgba(0,0,0,0.4) !important;
                border-radius: 12px !important;
                overflow: hidden !important;
              }

              /* 3. 🚨 修复播放按键比例，并准备吸底布局 */
              .-DaA0W_controls {
                width: 100% !important;
                max-width: 500px !important;
                flex-shrink: 0 !important;
                position: relative !important;
                left: 0 !important;
                transform: none !important;
              }

              /* 撤销猛药，恢复 1.1 倍的舒适触控尺寸 (原本是 0.5) */
              [class*="songMediaButton"] > svg,
              [class*="songMediaPlayButton"] > svg {
                scale: 1.1 !important;
                transition: none !important;
              }

              .UjzbJG_controls, .UjzbJG_bigControls {
                width: 100% !important;
                justify-content: space-between !important;
                padding: 0 4vw !important;
              }

              /* ========================================================= */
              /* 🚨 修复 size: [0, 0] 和 top: NaN 的物理坍塌 Bug */
              /* ========================================================= */
              .-DaA0W_lyric {
                flex: 1 1 0 !important;
                width: 100% !important;
                max-width: 500px !important;
                height: 100% !important;      /* 强迫占据所有高度 */
                min-height: 250px !important;
                margin: 0 auto !important;
                position: relative !important; /* 作为内部绝对定位的基准 */
                opacity: 1 !important;
              }

              /* 🚨 终极杀招：直接点名内部崩溃的 DOM 节点，强制它们 100% 铺满！ */
              .-DaA0W_lyric > div,
              .amll-lyric-player,
              .amll-lyric-player.dom {
                width: 100% !important;
                height: 100% !important;
                position: absolute !important;
                top: 0 !important;
                left: 0 !important;
              }

              /* 5. 底部附属菜单 (歌词切换等) */
              .-DaA0W_bottomControls {
                width: 100% !important;
                max-width: 500px !important;
                flex-shrink: 0 !important;
                justify-content: space-evenly !important;
                padding: 0 !important;
                margin-top: 1vh !important;
              }

              /* ========================================================= */
              /* 🪄 交互联动：完美的物理占位控制 */
              /* ========================================================= */

              /* 状态 A：当歌词打开时 (膨胀的歌词会负责把控件挤到底部) */
              .-DaA0W_horizontalLayout:not(.-DaA0W_hideLyric) .-DaA0W_cover {
                width: 25vw !important; /* 封面缩小到左侧 */
                margin: 0 auto 0 0 !important;
                border-radius: 8px !important;
              }
              .-DaA0W_horizontalLayout:not(.-DaA0W_hideLyric) .-DaA0W_lyric {
                display: block !important;
              }

              /* 状态 B：当歌词关闭时 (没有了歌词，由封面负责把控件挤到底部) */
              .-DaA0W_horizontalLayout.-DaA0W_hideLyric .-DaA0W_lyric {
                display: none !important;
              }
              .-DaA0W_horizontalLayout.-DaA0W_hideLyric .-DaA0W_cover {
                /* 核心魔法：上下 margin 设置 auto，封面会完美居中，并把底下的控件顶到屏幕最下方 */
                margin-top: auto !important;
                margin-bottom: auto !important;
              }
      `}</style>

      <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", overflow: "hidden" }}>
        <AppleMusicBackground />
        <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", zIndex: 1 }}>
          {mounted && <AMLL.PrebuiltLyricPlayer />}
        </div>
      </div>
    </>
  );
}