"""One-off asset regeneration: derive a proper monochrome (themed-icon) silhouette
from the existing adaptive icon foreground. Run manually after changing the app icon;
not wired into the Gradle build.

The foreground PNG bakes its own dark navy backdrop fill in as opaque pixels (not just
the logo), so a plain alpha threshold would silhouette the whole square. Instead this
also discriminates the near-black backdrop fill from the saturated/bright logo pixels
by RGB max-channel brightness, then masks only the logo as the monochrome shape.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
FOREGROUND = ROOT / "app/src/main/res/drawable-v24/ic_launcher_foreground.png"
MONOCHROME = ROOT / "app/src/main/res/drawable-v24/ic_launcher_monochrome.png"
BRIGHTNESS_THRESHOLD = 50

import numpy as np

fg = Image.open(FOREGROUND).convert("RGBA")
arr = np.array(fg)
rgb_max = arr[:, :, :3].max(axis=2)
alpha = arr[:, :, 3]
logo_mask = (alpha > 0) & (rgb_max > BRIGHTNESS_THRESHOLD)

mono_arr = np.zeros_like(arr)
mono_arr[:, :, 0] = 255
mono_arr[:, :, 1] = 255
mono_arr[:, :, 2] = 255
mono_arr[:, :, 3] = np.where(logo_mask, 255, 0).astype(np.uint8)

mono = Image.fromarray(mono_arr, mode="RGBA")
mono.save(MONOCHROME)
print(f"Wrote {MONOCHROME} ({mono.size[0]}x{mono.size[1]}), logo px={logo_mask.sum()}")
