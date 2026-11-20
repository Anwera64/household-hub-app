"""Tests for sheets.py's report helpers. They need opencv-python, which CI doesn't install, so run
them where the skill runs:

  python -m unittest discover -s .claude/skills/test-on-emulator/scripts -p "test_*.py"
"""
import os
import sys
import tempfile
import unittest

import cv2
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sheets  # noqa: E402


def stripes(w, h):
    """Black and white stripes 4 px wide: a blur turns them grey, compression doesn't."""
    f = np.zeros((h, w, 3), np.uint8)
    for x in range(0, w, 8):
        f[:, x : x + 4] = 255
    return f


def write_video(path, frames, fps):
    out = cv2.VideoWriter(path, cv2.VideoWriter_fourcc(*"MJPG"), fps, frames[0].shape[1::-1])
    for f in frames:
        out.write(f)
    out.release()


def read_all(path):
    return [f for _, f in sheets.read(path)]


class Shrink(unittest.TestCase):
    def test_shrink_turns_screenshots_into_jpgs_at_the_width(self):
        with tempfile.TemporaryDirectory() as d:
            src, out = os.path.join(d, "src"), os.path.join(d, "out")
            os.makedirs(src)
            cv2.imwrite(os.path.join(src, "step1.png"), stripes(1280, 2856))
            sheets.shrink(src, out, 720)
            im = cv2.imread(os.path.join(out, "step1.jpg"))
            self.assertIsNotNone(im)
            self.assertEqual(im.shape[1], 720)
            self.assertEqual(im.shape[0], round(2856 * 720 / 1280))


class Blur(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.src = os.path.join(self.dir.name, "step.avi")
        write_video(self.src, [stripes(320, 240) for _ in range(20)], 10)
        self.out = os.path.join(self.dir.name, "step.webm")

    def tearDown(self):
        self.dir.cleanup()

    def test_blur_blurs_the_band_and_leaves_the_rest(self):
        sheets.blur(self.src, 100, 140, self.out)
        f = read_all(self.out)[5]
        self.assertLess(f[105:135].std(), 20, "the band is blurred")
        self.assertGreater(f[0:90].std(), 80, "above it is not")
        self.assertGreater(f[150:240].std(), 80, "below it is not")

    def test_blur_keeps_the_timeline(self):
        sheets.blur(self.src, 100, 140, self.out)
        last_in = list(sheets.read(self.src))[-1][0]
        frames_out = len(read_all(self.out))
        self.assertAlmostEqual((frames_out - 1) / 30, last_in, delta=1 / 30 + 0.01)

    def test_blur_stops_at_the_time_given(self):
        sheets.blur(self.src, 100, 140, self.out, until=0.5)
        frames = read_all(self.out)
        self.assertLess(frames[3][105:135].std(), 20, "blurred before 0.5 s")
        self.assertGreater(frames[-1][105:135].std(), 80, "sharp again after it")


if __name__ == "__main__":
    unittest.main()
