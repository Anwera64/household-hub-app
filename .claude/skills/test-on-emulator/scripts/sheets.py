"""Frames from step videos, for reviewing them, and media for the report.

  python sheets.py sheet VIDEO_DIR OUT_DIR             one contact sheet of distinct frames per video
  python sheets.py frames VIDEO START END OUT          every frame between two times (at most 18), one strip
  python sheets.py shrink SRC_DIR OUT_DIR [WIDTH]      screenshots (PNG) as JPGs at WIDTH (default 720)
  python sheets.py blur VIDEO Y0 Y1 OUT.webm [UNTIL]   blur rows Y0..Y1 (until UNTIL seconds), as WebM

Needs opencv-python. Low thresholds on purpose: a band fading in or a bubble flashing for
0.4 s is a small change, and the sheet is the first place it can show. blur writes VP8 WebM
because OpenCV's own build has no H.264 encoder; browsers play both.
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


def shrink(src, out, width=720):
    os.makedirs(out, exist_ok=True)
    for name in sorted(n for n in os.listdir(src) if n.lower().endswith(".png")):
        im = cv2.imread(os.path.join(src, name))
        h, w = im.shape[:2]
        if w > width:
            im = cv2.resize(im, (width, round(h * width / w)), interpolation=cv2.INTER_AREA)
        cv2.imwrite(os.path.join(out, os.path.splitext(name)[0] + ".jpg"), im, [cv2.IMWRITE_JPEG_QUALITY, 82])
        print(name)


def blur(video, y0, y1, out, until=None):
    """Personal data (the calendar account on Profile) out of a video before it is shared.

    screenrecord writes frames only when the screen changes, so each output frame at 30 fps repeats
    the latest input frame: the video keeps its real timeline.
    """
    shots = list(read(video))
    h, w = shots[0][1].shape[:2]
    writer = cv2.VideoWriter(out, cv2.CAP_FFMPEG, cv2.VideoWriter_fourcc(*"VP80"), 30, (w, h))
    i = 0
    for k in range(int(shots[-1][0] * 30) + 1):
        t = k / 30
        while i + 1 < len(shots) and shots[i + 1][0] <= t:
            i += 1
        f = shots[i][1].copy()
        if until is None or t <= until:
            f[y0:y1] = cv2.GaussianBlur(f[y0:y1], (0, 0), 9)
        writer.write(f)
    writer.release()
    print(f"{out}: {shots[-1][0]:.1f}s")


if __name__ == "__main__":
    a = sys.argv[1:]
    if a[:1] == ["sheet"]:
        sheet(a[1], a[2])
    elif a[:1] == ["frames"]:
        frames(a[1], float(a[2]), float(a[3]), a[4])
    elif a[:1] == ["shrink"]:
        shrink(a[1], a[2], int(a[3]) if len(a) > 3 else 720)
    elif a[:1] == ["blur"]:
        blur(a[1], int(a[2]), int(a[3]), a[4], float(a[5]) if len(a) > 5 else None)
    else:
        print(__doc__)
        sys.exit(2)
