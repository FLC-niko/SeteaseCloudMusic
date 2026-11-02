"""Render the app's actual Compose pages on a foldable emulator and restore its configuration.

Build :app:assembleDebug :app:assembleDebugAndroidTest first.
Usage: python scripts/verify_foldable_layouts.py --adb D:/Android/Sdk/platform-tools/adb.exe
"""
import argparse
import hashlib
import json
import subprocess
import re
import time
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEST_CLASS = "com.example.seteasecloudmusic.AdaptiveLayoutsTest"
RUNNER = "com.luna.music.test/androidx.test.runner.AndroidJUnitRunner"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default="emulator-5556")
    parser.add_argument("--scenarios", default="inner,outer,landscape,outer-landscape,book,tabletop,resized,large-font,outer-large-font")
    parser.add_argument("--resume", action="store_true", help="Reuse passed scenarios only when source and APK hashes match")
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        raise SystemExit("This test harness changes emulator display settings; select an emulator serial.")
    scenarios = args.scenarios.split(",")
    allowed = {"inner", "outer", "landscape", "outer-landscape", "book", "tabletop", "resized", "large-font", "outer-large-font"}
    if not scenarios or any(label not in allowed for label in scenarios):
        parser.error("Select scenarios from: " + ", ".join(sorted(allowed)))
    output = ROOT / "artifacts/foldable/verified"
    output.mkdir(parents=True, exist_ok=True)

    def adb(*command, check=True):
        result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                                encoding="utf-8", errors="replace", check=check)
        return result.stdout + result.stderr

    original_font = adb("shell", "settings", "get", "system", "font_scale").strip()
    original_rotation = adb("shell", "cmd", "window", "user-rotation").strip()
    hinge_text = adb("emu", "sensor", "get", "hinge-angle0")
    hinge_match = re.search(r"=\s*([\d.]+)", hinge_text)
    original_hinge = hinge_match.group(1) if hinge_match else "180"
    size_match = re.search(r"Override size:\s*(\d+x\d+)", adb("shell", "wm", "size"))
    original_size = size_match.group(1) if size_match else "reset"
    state_dump = adb("shell", "dumpsys", "device_state")
    override_match = re.search(r"mOverrideState=Optional\[DeviceState\{identifier=(\d+)", state_dump)
    original_override = override_match.group(1) if override_match else "reset"
    report = {"scenarios": [], "sourceSha256": {}}
    sources = list((ROOT / "app/src/main/java").rglob("*.kt"))
    sources += [path for path in (ROOT / "amll-web-player/src").rglob("*") if path.suffix in {".jsx", ".js", ".css"}]
    report["sourceSha256"] = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in sources}
    report["apkSha256"] = {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in [
        ROOT / "app/build/outputs/apk/debug/app-debug.apk",
        ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
    ]}
    report["initialDeviceState"] = adb("shell", "cmd", "device_state", "print-state")
    if args.resume:
        prior = json.loads((output / "report.json").read_text(encoding="utf-8"))
        if any(prior.get(key) != report[key] for key in ["sourceSha256", "apkSha256"]):
            raise SystemExit("Source or APK changed; run the complete matrix again")
        report["scenarios"] = [item for item in prior["scenarios"] if item["passed"]]
    completed = {item["name"] for item in report["scenarios"]}

    try:
        print(adb("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk")), flush=True)
        print(adb("install", "-r", str(ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")), flush=True)
        # Prevent permission dialogs from replacing the deterministic local-library fixture.
        for permission in ["android.permission.READ_MEDIA_AUDIO", "android.permission.POST_NOTIFICATIONS"]:
            adb("shell", "pm", "grant", "com.luna.music", permission, check=False)
        for label in scenarios:
            if label in completed:
                print(f"{label}: reused PASS (identical source and APK)", flush=True)
                continue
            adb("shell", "wm", "size", "reset")
            adb("shell", "settings", "put", "system", "font_scale", "1.0")
            adb("shell", "cmd", "window", "user-rotation", "lock", "0")
            angle, state = (0, 0) if label.startswith("outer") else (90, 1) if label in {"book", "tabletop"} else (180, 2)
            adb("emu", "fold" if label.startswith("outer") else "unfold")
            adb("emu", "sensor", "set", "hinge-angle0", str(angle))
            adb("shell", "cmd", "device_state", "state", str(state))
            if label in {"landscape", "outer-landscape", "tabletop"}:
                adb("shell", "cmd", "window", "user-rotation", "lock", "1")
            if label == "resized":
                adb("shell", "wm", "size", "1240x1550")
            if label.endswith("large-font"):
                adb("shell", "settings", "put", "system", "font_scale", "1.5")
            adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
            # Folding can turn the new display off after the first wake event; wait for the switch.
            awake_checks = 0
            for _ in range(20):
                adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
                awake = "mWakefulness=Awake" in adb("shell", "dumpsys", "power")
                awake_checks = awake_checks + 1 if awake else 0
                if awake_checks >= 3:
                    break
                time.sleep(0.2)
            adb("shell", "wm", "dismiss-keyguard")
            # The launcher can request portrait on the outer display; settle rotation in our resizable activity.
            adb("shell", "am", "start", "-W", "-n", "com.luna.music/com.example.seteasecloudmusic.feature.main.MainActivity")
            # Display-specific rotation preferences can reset when the physical panel changes.
            target_rotation = "1" if label in {"landscape", "outer-landscape", "tabletop"} else "0"
            stable_frames = 0
            for _ in range(30):
                adb("shell", "cmd", "window", "user-rotation", "lock", target_rotation)
                frames = re.search(r"DisplayFrames w=(\d+) h=(\d+) r=(\d+)", adb("shell", "dumpsys", "window", "displays"))
                correct_rotation = frames and frames.group(3) == target_rotation
                stable_frames = stable_frames + 1 if correct_rotation else 0
                if stable_frames >= 3:
                    break
                time.sleep(0.3)
            if stable_frames < 3:
                raise SystemExit(f"Display rotation did not settle for {label}")
            adb("shell", "wm", "dismiss-keyguard")
            print(f"Rendering {label} ...", flush=True)
            selected = TEST_CLASS if label == "inner" else f"{TEST_CLASS}#allPagesRenderAndCapture"
            result = adb("shell", "am", "instrument", "-w", "-r", "-e", "class", selected,
                         "-e", "layoutLabel", label, RUNNER, check=False)
            (output / f"{label}-tests.log").write_text(result, encoding="utf-8")
            passed = "OK (" in result and "FAILURES!!!" not in result
            report["scenarios"].append({"name": label, "passed": passed,
                                        "deviceState": adb("shell", "cmd", "device_state", "print-state", check=False)})
            (output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"{label}: {'PASS' if passed else 'FAIL'}", flush=True)
            if not passed:
                print(result[-5000:], flush=True)
                raise SystemExit(f"Rendering failed for {label}")
        print(adb("pull", "/sdcard/Android/data/com.luna.music/files/foldable-qa", str(output)), flush=True)
    finally:
        adb("shell", "wm", "size", original_size, check=False)
        if original_font == "null":
            adb("shell", "settings", "delete", "system", "font_scale", check=False)
        else:
            adb("shell", "settings", "put", "system", "font_scale", original_font, check=False)
        adb("emu", "fold" if float(original_hinge) < 30 else "unfold", check=False)
        adb("emu", "sensor", "set", "hinge-angle0", original_hinge, check=False)
        adb("shell", "cmd", "device_state", "state", original_override, check=False)
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
        rotation = original_rotation.split()
        if rotation:
            adb("shell", "cmd", "window", "user-rotation", *rotation, check=False)
        (output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
