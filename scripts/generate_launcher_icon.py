"""One-off asset regeneration: builds the adaptive launcher icon layers from the full-bleed
source artwork (dark rounded tile with a light glyph). Run manually after changing the artwork;
not wired into the Gradle build.

Usage: python scripts/generate_launcher_icon.py path/to/flick.png
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable-v24"
SIZE = 1024
GLYPH_FRACTION = 0.56  # glyph bbox vs. canvas; adaptive safe zone is 66/108 = 0.61
LO, HI = 60, 200  # backdrop is <= ~45 max-channel, glyph ~240

src = Image.open(sys.argv[1]).convert("RGBA")
arr = np.array(src).astype(np.float32)
opaque = arr[:, :, 3] > 250
rgb_max = arr[:, :, :3].max(axis=2)
cov = np.clip((rgb_max - LO) / (HI - LO), 0, 1) * (arr[:, :, 3] / 255.0)
cov[~opaque & (arr[:, :, 3] < 250)] *= 0  # transparent tile corners are not glyph

ys, xs = np.where(cov > 0.5)
x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
glyph = cov[y0:y1, x0:x1]
scale = SIZE * GLYPH_FRACTION / max(glyph.shape)
w, h = round(glyph.shape[1] * scale), round(glyph.shape[0] * scale)
mask = Image.fromarray((glyph * 255).astype(np.uint8), "L").resize((w, h), Image.LANCZOS)

canvas = Image.new("L", (SIZE, SIZE), 0)
canvas.paste(mask, ((SIZE - w) // 2, (SIZE - h) // 2))
alpha = np.array(canvas)

def rgba(color):
    out = np.zeros((SIZE, SIZE, 4), np.uint8)
    out[:, :, :3] = color
    out[:, :, 3] = alpha
    return Image.fromarray(out, "RGBA")

rgba((244, 244, 244)).save(RES / "ic_launcher_foreground.png")
rgba((255, 255, 255)).save(RES / "ic_launcher_monochrome.png")

top, bottom = np.array([35, 35, 42]), np.array([22, 23, 29])
t = np.linspace(0, 1, SIZE)[:, None, None]
grad = (top * (1 - t) + bottom * t).astype(np.uint8)
bg = np.concatenate([np.repeat(grad, SIZE, axis=1), np.full((SIZE, SIZE, 1), 255, np.uint8)], axis=2)
Image.fromarray(bg, "RGBA").save(RES / "ic_launcher_background.png")

src.resize((SIZE, SIZE), Image.LANCZOS).save(RES / "ic_launcher_legacy.png")
print("glyph bbox", (x0, y0, x1, y1), "->", (w, h))
