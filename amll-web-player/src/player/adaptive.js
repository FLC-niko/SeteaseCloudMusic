export function resolvePlayerLayout(width, height, fold) {
  if (fold && fold.right >= fold.left && fold.bottom >= fold.top) {
    if (fold.horizontal && fold.top > 0 && fold.bottom < height) return "tabletop";
    if (!fold.horizontal && fold.left > 0 && fold.right < width) return "book";
  }
  return width >= 600 || (width >= 480 && height < 480) ? "wide" : "compact";
}

export function playbackPosition(sync, now) {
  const elapsed = sync.playing ? Math.max(0, now - sync.at) : 0;
  const position = Math.max(0, Math.round(sync.position + elapsed));
  return sync.duration > 0 ? Math.min(position, sync.duration) : position;
}

export function formatTime(milliseconds) {
  const seconds = Math.max(0, Math.floor(milliseconds / 1000));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
