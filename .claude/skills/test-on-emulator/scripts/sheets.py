"""Frames from step videos, for reviewing them.

  python sheets.py sheet VIDEO_DIR OUT_DIR       one contact sheet of distinct frames per video
  python sheets.py frames VIDEO START END OUT    every frame between two times (at most 18), one strip

Needs opencv-python. Low thresholds on purpose: a band fading in or a bubble flashing for
0.4 s is a small change, and the sheet is the first place it can show.
"""
import os
import sys

import cv2
import numpy as np

TW, COLS = 300, 6


def grid(frames, out):
    h, w = frames[0][1].shape[:2]
    th = int(h * TW / w)
    rows = (len(frames) + COLS - 1) // COLS
    sheet = np.full((rows * (th + 36), COLS * TW, 3), 255, np.uint8)
    for i, (t, f) in enumerate(frames):
        r, c = divmod(i, COLS)
        y, x = r * (th + 36), c * TW
        sheet[y + 36 : y + 36 + th, x : x + TW] = cv2.resize(f, (TW, th))
        cv2.putText(sheet, f"#{i + 1} {t:.2f}s", (x + 8, y + 26), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 200), 2)
        cv2.rectangle(sheet, (x, y + 36), (x + TW - 1, y + 36 + th - 1), (160, 160, 160), 1)
    cv2.imwrite(out, sheet)


def read(video):
    cap = cv2.VideoCapture(video)
    while True:
        ok, f = cap.read()
        if not ok:
            return
        yield cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0, f


def sheet(src, out):
    os.makedirs(out, exist_ok=True)
    for name in sorted(n for n in os.listdir(src) if n.endswith(".mp4")):
        kept, last, last_t, final = [], None, -1.0, None
        for t, f in read(os.path.join(src, name)):
            small = cv2.cvtColor(cv2.resize(f, (90, 200)), cv2.COLOR_BGR2GRAY).astype(np.int16)
            if last is None or (np.abs(small - last).mean() > 1.0 and t - last_t > 0.3):
                kept.append((t, f))
                last, last_t = small, t
            final = (t, f)
        if final and kept[-1][0] != final[0]:
            kept.append(final)
        grid(kept, os.path.join(out, name.replace(".mp4", ".png")))
        print(f"{name}: {len(kept)} distinct frames, {final[0]:.1f}s long")


def frames(video, start, end, out):
    picked = [(t, f) for t, f in read(video) if start <= t <= end]
    if len(picked) > 18:
        picked = [picked[i] for i in np.linspace(0, len(picked) - 1, 18).round().astype(int)]
    grid(picked, out)
    print(" ".join(f"{t:.2f}" for t, _ in picked))


if __name__ == "__main__":
    if sys.argv[1] == "sheet":
        sheet(sys.argv[2], sys.argv[3])
    else:
        frames(sys.argv[2], float(sys.argv[3]), float(sys.argv[4]), sys.argv[5])
