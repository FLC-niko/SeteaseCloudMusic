"""Run and inspect an Android emulator without Android Studio (Python standard library only)."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.luna.music"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", default=os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT"))
    parser.add_argument("--serial", default="emulator-5556")
    commands = parser.add_subparsers(dest="command", required=True)
    start = commands.add_parser("start")
    start.add_argument("--avd", default="Pixel_10_Pro_Fold")
    start.add_argument("--gpu", default="host", choices=["host", "software", "auto"])
    start.add_argument("--vulkan", action="store_true", help="Enable Vulkan; disabled by default to avoid host GL interoperability issues")
    commands.add_parser("status")
    install = commands.add_parser("install")
    install.add_argument("--apk", type=Path, default=ROOT / "app/build/outputs/apk/debug/app-debug.apk")
    commands.add_parser("launch")
    capture = commands.add_parser("capture")
    capture.add_argument("--output", type=Path, default=ROOT / "artifacts/emulator/screen.png")
    capture.add_argument("--display-id", help="SurfaceFlinger physical display ID; otherwise select the active display")
    logs = commands.add_parser("logs")
    logs.add_argument("--output", type=Path, default=ROOT / "artifacts/emulator/logcat.txt")
    commands.add_parser("ui")
    commands.add_parser("stop")
    args = parser.parse_args()
    if not re.fullmatch(r"emulator-\d+", args.serial):
        parser.error("Select an emulator serial, e.g. emulator-5556")
    sdk = args.sdk
    if not sdk:
        properties = ROOT / "local.properties"
        if properties.exists():
            match = re.search(r"^sdk.dir=(.+)$", properties.read_text(), re.M)
            if match:
                sdk = match.group(1).replace("\\:", ":").replace("\\\\", "\\")
    if not sdk:
        parser.error("Pass --sdk or set ANDROID_HOME")
    suffix = ".exe" if os.name == "nt" else ""
    adb_path = str(Path(sdk) / "platform-tools" / f"adb{suffix}")

    def adb(*command, timeout=30, check=True):
        return subprocess.run([adb_path, "-s", args.serial, *command], capture_output=True,
                              timeout=timeout, check=check)

    def text(result):
        return (result.stdout + result.stderr).decode("utf-8", errors="replace").strip()

    if args.command == "start":
        state = adb("get-state", check=False)
        if state.returncode == 0 and state.stdout.strip() == b"device":
            print(f"{args.serial} is already running")
            return
        output = ROOT / "artifacts/emulator"
        output.mkdir(parents=True, exist_ok=True)
        executable = str(Path(sdk) / "emulator" / f"emulator{suffix}")
        port = args.serial.removeprefix("emulator-")
        with (output / "stdout.log").open("wb") as stdout, (output / "stderr.log").open("wb") as stderr:
            process = subprocess.Popen([executable, "-avd", args.avd, "-port", port, "-no-window",
                                        "-no-audio", "-no-boot-anim", "-no-snapshot", "-gpu", args.gpu,
                                        *([] if args.vulkan else ["-feature", "-Vulkan"])],
                                       stdout=stdout, stderr=stderr,
                                       creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        # Poll a specific live child rather than treating an observation timeout as an exit.
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise SystemExit(f"Emulator exited ({process.returncode}); inspect {output}")
            try:
                if adb("shell", "getprop", "sys.boot_completed", timeout=5, check=False).stdout.strip() == b"1":
                    print(f"{args.serial} ready; PID {process.pid}; logs: {output}")
                    return
            except subprocess.TimeoutExpired:
                pass
            time.sleep(1)
        raise SystemExit(f"Startup is still pending; PID {process.pid}; inspect {output} before restarting")
    elif args.command == "status":
        for command in [("get-state",), ("shell", "getprop", "sys.boot_completed"),
                        ("shell", "wm", "size"), ("shell", "cmd", "device_state", "print-state")]:
            print(" ".join(command), text(adb(*command)))
    elif args.command == "install":
        print(text(adb("install", "-r", str(args.apk.resolve()), timeout=120)))
    elif args.command == "launch":
        print(text(adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")))
        print(text(adb("shell", "wm", "dismiss-keyguard")))
        print(text(adb("shell", "am", "start", "-W", "-n",
                       f"{PACKAGE}/com.example.seteasecloudmusic.feature.main.MainActivity")))
    elif args.command == "capture":
        display_id = args.display_id
        if not display_id:
            # Default screencap may select the inactive inner display on foldables.
            displays = text(adb("shell", "dumpsys", "display"))
            for line in displays.splitlines():
                if "DisplayDeviceInfo{" in line and "state ON" in line:
                    match = re.search(r'local:(\d+)', line)
                    if match:
                        display_id = match.group(1)
                        break
        command = ["exec-out", "screencap", "-p"]
        if display_id:
            command += ["-d", display_id]
        result = adb(*command)
        if not result.stdout.startswith(b"\x89PNG\r\n\x1a\n"):
            raise SystemExit(f"Screenshot did not return a PNG: {text(result)[:300]}")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(result.stdout)
        print(f"PNG: {args.output.resolve()} (display {display_id or 'default'})")
    elif args.command == "logs":
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(adb("logcat", "-d", "-v", "threadtime").stdout)
        print(args.output.resolve())
    elif args.command == "ui":
        print(text(adb("shell", "uiautomator", "dump", "/sdcard/window.xml")))
        print(text(adb("shell", "cat", "/sdcard/window.xml")))
    elif args.command == "stop":
        print(text(adb("emu", "kill")))


if __name__ == "__main__":
    main()
