import React from "react";
import ReactDOM from "react-dom/client";
import PlayerApp from "./player/PlayerApp";

window.__playerErrors = [];
function reportError(message) {
  window.__playerErrors = [...window.__playerErrors.slice(-9), message];
  console.error("Player error:", message);
}
window.addEventListener("error", event => reportError(event.message));
window.addEventListener("unhandledrejection", event => reportError(String(event.reason)));

ReactDOM.createRoot(document.getElementById("root")).render(
  <PlayerApp />
);
