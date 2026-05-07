import React, { useEffect, useState } from "react";
import { useSetAtom, useAtomValue } from "jotai";
import * as AMLL from "@applemusic-like-lyrics/react-full";
import "@applemusic-like-lyrics/react-full/style.css";
import { parseLrc, parseYrc } from "@applemusic-like-lyrics/lyric";
import { postToNative, createBridge } from "./bridge";
import { Theme } from "@radix-ui/themes";
import "@radix-ui/themes/styles.css";

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
    setIsLyricPageOpened(true); // 默认收起歌词，展示大封面
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
          break;
      }
    });

    postToNative({ type: "WEB_READY" });
    return () => dispose();
  }, []);

  return (
    <Theme appearance="dark" style={{ width: '100%', height: '100%' }}>
      <style>{`
        html, body, #root {
          width: 100vw; height: 100vh; margin: 0; padding: 0;
          overflow: hidden; background: #000; touch-action: none;
        }
        ._8f42YG_background { display: none !important; opacity: 0 !important; }
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