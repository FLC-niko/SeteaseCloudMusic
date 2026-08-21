import React, { useEffect, useState } from "react";
import { useSetAtom, useAtomValue } from "jotai";
import * as AMLL from "@applemusic-like-lyrics/react-full";
import "@applemusic-like-lyrics/react-full/style.css";
import { parseLrc, parseYrc } from "@applemusic-like-lyrics/lyric";
import { parseTTML } from "@applemusic-like-lyrics/ttml";
import { postToNative, createBridge } from "./bridge";
import { Theme } from "@radix-ui/themes";
import "@radix-ui/themes/styles.css";

const demoLrc = `[00:00.00]Setease Cloud Music
[00:04.00]空间扭曲魔法 2.0 部署完毕！
[00:08.00]按钮尺寸恢复，封面呈现！`;

// 绝美底层毛玻璃背景
const AppleMusicBackground = () => {
  const coverUrl = useAtomValue(AMLL.musicCoverAtom);
  return (
    <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", overflow: "hidden", zIndex: 0 }}>
      <div style={{
        position: "absolute", top: "-20%", left: "-20%", width: "140%", height: "140%",
        backgroundImage: `url(${coverUrl})`, backgroundSize: "cover", backgroundPosition: "center",
        filter: "blur(60px) saturate(180%)", transition: "background-image 0.5s ease"
      }} />
      <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", backgroundColor: "rgba(0, 0, 0, 0.35)" }} />
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
  const setOnLyricLineClick = useSetAtom(AMLL.onLyricLineClickAtom);

  const setStaticMode = useSetAtom(AMLL.lyricBackgroundStaticModeAtom);
  const setIsLyricPageOpened = useSetAtom(AMLL.isLyricPageOpenedAtom);
  const setShowBottomControl = useSetAtom(AMLL.showBottomControlAtom);
  const setPlayerControlsType = useSetAtom(AMLL.playerControlsTypeAtom);
  const setLyricSizePreset = useSetAtom(AMLL.lyricSizePresetAtom);
  const setLyricFontWeight = useSetAtom(AMLL.lyricFontWeightAtom);

  const [mounted, setMounted] = useState(false);

  const currentTimeRef = React.useRef(0);
  const syncTimeOffsetRef = React.useRef(0);
  const isPlayingRef = React.useRef(false);

  useEffect(() => {
    let frameId;
    const ticker = () => {
      if (isPlayingRef.current) {
        const now = performance.now();
        const delta = now - syncTimeOffsetRef.current;
        setCurrentTimeMs(currentTimeRef.current + delta);
      }
      frameId = requestAnimationFrame(ticker);
    };
    frameId = requestAnimationFrame(ticker);
    return () => cancelAnimationFrame(frameId);
  }, [setCurrentTimeMs]);

  useEffect(() => {
    setMounted(true);
    setStaticMode(true);
    setIsLyricPageOpened(true); // 默认打开歌词页面
    setShowBottomControl(false);
    setPlayerControlsType("none");

    // 设置超大字号与极粗字重 (900 Black)，完美契合 Apple Music 移动端超大粗体歌词
    setLyricSizePreset(AMLL.LyricSizePreset.Huge);
    setLyricFontWeight(900);

    setOnPlayOrResume(() => () => postToNative({ type: "TOGGLE_PLAY" }));
    setOnRequestNextSong(() => () => postToNative({ type: "NEXT_TRACK" }));
    setOnRequestPrevSong(() => () => postToNative({ type: "PREV_TRACK" }));
    setOnSeekPosition(() => (timeMs) => postToNative({ type: "SEEK_TO", payload: { timeMs } }));
    setOnLyricLineClick(() => (evt) => {
      const timeMs = evt?.line?.getLine()?.startTime;
      if (typeof timeMs === "number") {
        postToNative({ type: "SEEK_TO", payload: { timeMs } });
      }
    });

    setLyricLines(parseLrc(demoLrc));
    setMusicName("Setease Player");
    setMusicArtists(["Zhao Haixiang"]);
    setMusicCover("https://picsum.photos/500/500");

    const dispose = createBridge((msg) => {
      if (!msg?.type) return;
      switch (msg.type) {
        case "SET_PLAYBACK":
          currentTimeRef.current = (msg.payload?.currentTimeMs ?? 0) | 0;
          syncTimeOffsetRef.current = performance.now();
          isPlayingRef.current = !!msg.payload?.playing;

          setPlaying(isPlayingRef.current);
          setCurrentTimeMs(currentTimeRef.current);
          break;
        case "SET_TRACK":
          currentTimeRef.current = 0;
          syncTimeOffsetRef.current = performance.now();
          setCurrentTimeMs(0);

          if (typeof msg.payload?.lrc === "string") {
            const raw = msg.payload.lrc.trim();
            if (raw) {
              if (raw.startsWith("<tt") || raw.includes("<tt ") || raw.includes("<div")) {
                try {
                  setLyricLines(parseTTML(raw));
                } catch {
                  setLyricLines(parseLrc(raw));
                }
              } else if (raw.includes("(") && raw.includes(")") && raw.includes("[")) {
                try {
                  setLyricLines(parseYrc(raw));
                } catch {
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
          break;
      }
    });

    postToNative({ type: "WEB_READY" });
    return () => dispose();
  }, []);

  return (
    <Theme appearance="dark" style={{ width: "100%", height: "100%" }}>
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

        /* 隐藏 react-full 内置的重型 Pixi 背景以提升 WebView 渲染性能 */
        ._8f42YG_background {
          display: none !important;
          opacity: 0 !important;
        }

        /* 全局穿透强制设置超大歌词字号与极致加粗 (900 Heavy) */
        :root {
          --amll-lp-font-size: clamp(30px, 4.2vh, 42px) !important;
        }

        .amll-lyric-player {
          --amll-lp-font-size: clamp(30px, 4.2vh, 42px) !important;
          font-size: clamp(30px, 4.2vh, 42px) !important;
          font-weight: 900 !important;
          -webkit-text-stroke: 0.5px currentColor;
          letter-spacing: -0.015em !important;
        }

        .amll-lyric-player * {
          box-sizing: border-box !important;
        }

        /* 舒展适中的行距与内边距控制 */
        [class*="lyricLine"],
        .FmKaba_lyricLine,
        .KxF9Iq_lyricLine {
          font-size: clamp(30px, 4.2vh, 42px) !important;
          font-weight: 900 !important;
          -webkit-text-stroke: 0.5px currentColor;
          line-height: 1.22em !important;
          padding: 5px 18px !important;
          margin: 0 !important;
          height: auto !important;
          min-height: 0 !important;
        }

        [class*="lyricMainLine"],
        .FmKaba_lyricMainLine,
        .KxF9Iq_lyricMainLine {
          font-size: inherit !important;
          font-weight: inherit !important;
          line-height: 1.22em !important;
          margin: 0 !important;
          padding: 0 !important;
        }

        /* 彻底清除 AMLL 原生样式中 span 的 1em 外扩边距（杜绝容器被撑高） */
        [class*="lyricMainLine"] span,
        [class*="lyricMainLine"] > span,
        .FmKaba_lyricMainLine span,
        .FmKaba_wordBody,
        .FmKaba_emphasizeWrapper,
        .FmKaba_emphasize,
        .KxF9Iq_lyricMainLine span {
          margin: 0 !important;
          padding: 0 !important;
          line-height: inherit !important;
        }

        /* 隐藏无内容的副歌词（翻译/音译行），杜绝幽灵高度 */
        [class*="lyricSubLine"]:empty,
        .FmKaba_lyricSubLine:empty,
        .KxF9Iq_lyricSubLine:empty {
          display: none !important;
          margin: 0 !important;
          padding: 0 !important;
          height: 0 !important;
          min-height: 0 !important;
        }

        [class*="lyricSubLine"]:not(:empty),
        .FmKaba_lyricSubLine:not(:empty),
        .KxF9Iq_lyricSubLine:not(:empty) {
          font-size: clamp(15px, 2.2vh, 22px) !important;
          font-weight: 600 !important;
          line-height: 1.2em !important;
          opacity: 0.75 !important;
          margin-top: 4px !important;
          padding: 0 !important;
        }
      `}</style>

      <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", overflow: "hidden" }}>
        <AppleMusicBackground />
        <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", zIndex: 1 }}>
          {mounted && (
            <AMLL.PrebuiltLyricPlayer
              style={{ width: "100%", height: "100%" }}
            />
          )}
        </div>
      </div>
    </Theme>
  );
}