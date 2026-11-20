#!/usr/bin/env bash
# Drive an Android emulator for a PR's manual test. Git Bash or Linux.
#
#   emu.sh pick                  pin a running emulator, or boot the first AVD (prints the serial)
#   emu.sh model                 make sure the desktop's model answers (starts it on the PC)
#   emu.sh ui                    visible text, labels and bounds from the UI dump
#   emu.sh tap-text TEXT         tap the centre of the first node whose text or label contains TEXT
#   emu.sh pin                   type the dev PIN from $HUB_DEV_PIN on the PIN pad (exit 2 if unset: ask)
#   emu.sh tap X Y | text STR | key CODE | swipe X1 Y1 X2 Y2 [MS]
#   emu.sh back                  system Back (closes the keyboard first if it is open, then Back once)
#   emu.sh wait-for TEXT [SECS]  poll the dump until TEXT shows, in any case (default 20 s); exit 1 on timeout
#   emu.sh lock                  power off, wake and dismiss the keyguard
#   emu.sh shot NAME             screenshot into $EMU_OUT/NAME.png
#   emu.sh burst NAME [N]        N timed screenshots (default 12), NAME-01-<ms>ms.png…: for timing
#   emu.sh rec NAME | stop       record one step (under 3 minutes) into $EMU_OUT/NAME.mp4
#   emu.sh logwatch              stream the dev app's log lines (for a Monitor); all lines, timed,
#                                go to $EMU_OUT/applog.txt; a new logwatch stops the last one
#   emu.sh logstop               stop the log watcher
#
# EMU_OUT defaults to the app repo's gitignored out/test-videos.
set -u
export MSYS_NO_PATHCONV=1
APP=com.homelab.household.dev
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../../.." && pwd)"
EMU_OUT="${EMU_OUT:-$REPO/out/test-videos}"
STATE="${TMPDIR:-/tmp}/emu-sh-state"
mkdir -p "$EMU_OUT" "$STATE"
SDK="${ANDROID_HOME:-${LOCALAPPDATA:-$HOME}/Android/Sdk}"
[ -f "$STATE/serial" ] && export ANDROID_SERIAL="${ANDROID_SERIAL:-$(cat "$STATE/serial")}"

running_emulator() { adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}'; }

dump() {
  adb shell uiautomator dump /sdcard/emu-ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/emu-ui.xml | tr '>' '\n' | grep -E '(text|content-desc)="[^"]+"'
}

# Tap the centre of a dump line's bounds.
tap_center() {
  local x1 y1 x2 y2
  read -r x1 y1 x2 y2 <<<"$(echo "$1" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | paste -sd' ')"
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

# Stop the log watcher's adb, if one is recorded. A stopped Monitor can leave it running, and a
# second one would then write every line twice.
stop_watch() {
  [ -f "$STATE/logwatch.pid" ] || return 0
  kill "$(cat "$STATE/logwatch.pid")" 2>/dev/null
  rm -f "$STATE/logwatch.pid"
}

cmd="${1:-}"; shift || true
case "$cmd" in
  pick)
    s="$(running_emulator)"
    if [ -z "$s" ]; then
      avd="$("$SDK/emulator/emulator" -list-avds | head -1)"
      [ -n "$avd" ] || { echo "no AVD found"; exit 1; }
      echo "1/2 booting $avd"
      nohup "$SDK/emulator/emulator" -avd "$avd" -no-snapshot-save >"$STATE/emulator.log" 2>&1 &
      for _ in $(seq 1 60); do s="$(running_emulator)"; [ -n "$s" ] && break; sleep 3; done
      [ -n "$s" ] || { echo "emulator did not appear"; exit 1; }
    fi
    for _ in $(seq 1 60); do
      [ "$(adb -s "$s" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break; sleep 3
    done
    echo "$s" > "$STATE/serial"
    echo "2/2 using $s ($(adb -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r'))"
    ;;
  model)
    if curl -s -m 3 http://localhost:11434/api/version >/dev/null; then echo "model host answers"; exit 0; fi
    script="${HOMELAB_REPO:-$REPO/../Homelab}/hosts/desktop/startup_homelab.ps1"
    [ -f "$script" ] || { echo "Ollama is down and $script is missing: not on the PC?"; exit 1; }
    echo "1/2 starting Docker and the model"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$script" 2>/dev/null || echo "$script")"
    for i in $(seq 1 60); do
      curl -s -m 3 http://localhost:11434/api/version >/dev/null && { echo "2/2 model host answers"; exit 0; }
      printf '\r   waiting %ss' "$((i * 5))"; sleep 5
    done
    echo; echo "model host still not answering"; exit 1
    ;;
  ui)
    dump | grep -oE '(text|content-desc)="[^"]+"|bounds="[^"]+"' | paste -sd' ' | sed 's/ text=/\ntext=/g; s/ content-desc=/\ncontent-desc=/g'
    ;;
  tap-text)
    line="$(dump | grep -F "$1" | head -1)"
    [ -n "$line" ] || { echo "not on screen: $1"; exit 1; }
    tap_center "$line"
    ;;
  pin)
    # The dev hub's PIN comes from this PC's environment only: never from the repo, a file or a log.
    [ -n "${HUB_DEV_PIN:-}" ] || { echo "HUB_DEV_PIN is not set: ask for the dev PIN"; exit 2; }
    pad="$(dump)"
    for digit in $(printf '%s' "$HUB_DEV_PIN" | grep -o .); do
      key="$(printf '%s\n' "$pad" | grep -F "text=\"$digit\"" | head -1)"
      [ -n "$key" ] || { echo "the PIN pad is not on screen"; exit 1; }
      tap_center "$key"
    done
    echo "typed the dev PIN (${#HUB_DEV_PIN} digits)"
    ;;
  tap) adb shell input tap "$1" "$2" ;;
  text) adb shell input text "${1// /%s}" ;;
  key) adb shell input keyevent "$1" ;;
  swipe) adb shell input swipe "$1" "$2" "$3" "$4" "${5:-250}" ;;
  back)
    if adb shell dumpsys input_method | grep -q 'mInputShown=true'; then adb shell input keyevent 111; sleep 0.5; fi
    adb shell input keyevent 4
    ;;
  wait-for)
    end=$((SECONDS + ${2:-20}))
    # Any case (labels dump in capitals), through awk: Git Bash's grep aborts on -i.
    while [ $SECONDS -lt $end ]; do
      dump | awk -v s="$1" 'index(tolower($0), tolower(s)) { f = 1 } END { exit !f }' && { echo "seen: $1"; exit 0; }
      sleep 0.5
    done
    echo "timed out waiting for: $1"; exit 1
    ;;
  lock)
    adb shell input keyevent 26; sleep 3; adb shell input keyevent 224; sleep 1
    adb shell wm dismiss-keyguard 2>/dev/null || adb shell input keyevent 82
    ;;
  shot) adb exec-out screencap -p > "$EMU_OUT/$1.png" && echo "$EMU_OUT/$1.png" ;;
  burst)
    # For timing: a recording can hold a frame until the next UI dump, screenshots can't.
    n="${2:-12}"
    t0=$(date +%s%N)
    for i in $(seq 1 "$n"); do
      ms=$((($(date +%s%N) - t0) / 1000000))
      adb exec-out screencap -p >"$EMU_OUT/$1-$(printf %02d "$i")-${ms}ms.png"
    done
    echo "$n screenshots: $EMU_OUT/$1-01-…png to $1-$(printf %02d "$n")-…png"
    ;;
  rec)
    read -r w h <<<"$(adb shell wm size | tail -1 | grep -oE '[0-9]+x[0-9]+' | tr x ' ')"
    size="$(((w / 2) / 2 * 2))x$(((h / 2) / 2 * 2))"
    adb shell rm -f "/sdcard/$1.mp4"
    nohup adb shell screenrecord --size "$size" --bit-rate 6000000 --time-limit 180 "/sdcard/$1.mp4" >/dev/null 2>&1 &
    echo "$1" > "$STATE/rec"; sleep 1; echo "recording $1 at $size"
    ;;
  stop)
    n="$(cat "$STATE/rec")"
    adb shell pkill -INT screenrecord; sleep 3
    # MSYS_NO_PATHCONV keeps /sdcard intact, so the local path needs converting by hand on Windows.
    local_path="$(cygpath -m "$EMU_OUT/$n.mp4" 2>/dev/null || echo "$EMU_OUT/$n.mp4")"
    adb pull "/sdcard/$n.mp4" "$local_path" >/dev/null && echo "$EMU_OUT/$n.mp4"
    ;;
  logwatch)
    pid="$(adb shell pidof "$APP" | tr -d '\r')"
    [ -n "$pid" ] || { echo "$APP is not running"; exit 1; }
    stop_watch
    # adb writes to the file, never to a pipe, so every line is kept with its time. The filtered
    # view follows the file and ends when adb does.
    log="$EMU_OUT/applog.txt"
    touch "$log"
    from=$(($(wc -c <"$log") + 1))
    adb logcat -T 1 --pid="$pid" -v time >>"$log" 2>&1 &
    watcher=$!
    echo "$watcher" >"$STATE/logwatch.pid"
    trap 'kill "$watcher" 2>/dev/null; [ "$(cat "$STATE/logwatch.pid" 2>/dev/null)" = "$watcher" ] && rm -f "$STATE/logwatch.pid"' EXIT
    trap 'exit 0' INT TERM
    # In the background with wait, so a signal ends the watch at once instead of after the pipeline.
    tail -c +"$from" --pid="$watcher" -f "$log" | grep --line-buffered -E '/(App|Auth|Chat|Hub|HttpClient|Network)|AndroidRuntime|FATAL| E/' &
    wait $!
    ;;
  logstop) stop_watch; echo "log watch stopped" ;;
  *) awk 'NR > 1 && /^#/ { print; next } NR > 1 { exit }' "$0"; exit 1 ;;
esac
