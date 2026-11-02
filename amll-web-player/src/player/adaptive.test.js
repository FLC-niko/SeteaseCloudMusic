import test from "node:test";
import assert from "node:assert/strict";
import { resolvePlayerLayout, playbackPosition, formatTime } from "./adaptive.js";

test("outer display, unfolded display and short landscape use the appropriate layout", () => {
  assert.equal(resolvePlayerLayout(412, 900), "compact");
  assert.equal(resolvePlayerLayout(599, 900), "compact");
  assert.equal(resolvePlayerLayout(600, 900), "wide");
  assert.equal(resolvePlayerLayout(840, 900), "wide");
  assert.equal(resolvePlayerLayout(540, 360), "wide");
});
test("book and tabletop layouts follow separating folds inside the actual WebView", () => {
  assert.equal(resolvePlayerLayout(840, 900, { horizontal: false, left: 410, right: 430, top: 0, bottom: 900 }), "book");
  assert.equal(resolvePlayerLayout(840, 900, { horizontal: true, left: 0, right: 840, top: 440, bottom: 460 }), "tabletop");
  assert.equal(resolvePlayerLayout(360, 900, { horizontal: false, left: -400, right: -380, top: 0, bottom: 900 }), "compact");
});
test("playback interpolation survives layout changes and respects pause, seek and duration", () => {
  const playing = { position: 12000, duration: 180000, playing: true, at: 1000 };
  assert.equal(playbackPosition(playing, 1250), 12250);
  assert.equal(playbackPosition({ ...playing, playing: false }, 1250), 12000);
  assert.equal(playbackPosition({ ...playing, position: 179900 }, 1250), 180000);
  assert.equal(playbackPosition({ ...playing, position: 60000, at: 1250 }, 1250), 60000);
});
test("elapsed and remaining time display clamp negative values", () => {
  assert.equal(formatTime(-1), "0:00");
  assert.equal(formatTime(65000), "1:05");
});
