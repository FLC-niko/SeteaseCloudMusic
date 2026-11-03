// Web <-> Native message bridge for SeteaseCloudMusic + AMLL

/**
 * Native -> Web message examples:
 * {
 *   type: "SET_TRACK",
 *   payload: {
 *     id: "song-id",
 *     title: "Song",
 *     artist: "Artist",
 *     album: "Album",
 *     coverUrl: "https://...",
 *     ttml: "<tt ...>...</tt>"
 *   }
 * }
 * {
 *   type: "SET_PLAYBACK",
 *   payload: { currentTimeMs: 12345, durationMs: 240000, playing: true }
 * }
 *
 * Web -> Native message examples:
 * { type: "SEEK_TO", payload: { timeMs: 54321 } }
 * { type: "TOGGLE_PLAY" }
 */

export function createBridge(onNativeMessage) {
  // 1) Android injects events via evaluateJavascript -> window.dispatchEvent(new CustomEvent(...))
  const onCustomEvent = (e) => {
    if (!e || !e.detail) return;
    onNativeMessage(e.detail);
  };
  window.addEventListener("scm-native-message", onCustomEvent);

  // 2) Fallback: postMessage channel
  const onWindowMessage = (e) => {
    if (!e || !e.data) return;
    onNativeMessage(e.data);
  };
  window.addEventListener("message", onWindowMessage);
  window.__scmBridgeReady = true;

  return () => {
    window.__scmBridgeReady = false;
    window.removeEventListener("scm-native-message", onCustomEvent);
    window.removeEventListener("message", onWindowMessage);
  };
}

export function postToNative(message) {
  const text = JSON.stringify(message);

  // Android JavascriptInterface (recommended)
  if (window.SCMBridge && typeof window.SCMBridge.postMessage === "function") {
    window.SCMBridge.postMessage(text);
    return;
  }

  // WebView postMessage fallback
  if (window.ReactNativeWebView && typeof window.ReactNativeWebView.postMessage === "function") {
    window.ReactNativeWebView.postMessage(text);
    return;
  }

  // Browser debug fallback
  console.log("[SCM->Native]", message);
}
