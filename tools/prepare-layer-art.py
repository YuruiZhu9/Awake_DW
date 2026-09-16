"""Derive runtime midground layer assets for the alpha13 scene (0.7.0).

Resizes the cleaned source PNG to the runtime budget (810x1440), re-clears
resize-induced haze, and writes the asset referenced by ThemeArtwork.
Re-runnable; original sources stay untouched in images/Lolita/.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "lolita"
OUT.mkdir(parents=True, exist_ok=True)

SIZE = (810, 1440)
HAZE_THRESHOLD = 8

JOBS = [
    ("images/Lolita/中景装饰层-clean.png", "night_midground.png"),
]


def run() -> None:
    for source, output in JOBS:
        src_path = ROOT / source
        img = Image.open(src_path).convert("RGBA")
        img = img.resize(SIZE, Image.LANCZOS)
        alpha = img.getchannel("A")
        hist = alpha.histogram()
        img.putalpha(alpha.point(lambda v: 0 if 0 < v < HAZE_THRESHOLD else v))
        dst_path = OUT / output
        img.save(dst_path, optimize=True)
        size_kb = dst_path.stat().st_size / 1024
        print(f"{source} -> {dst_path.name}: {SIZE[0]}x{SIZE[1]}, {size_kb:.0f} KB, haze-cleared < {HAZE_THRESHOLD}")


if __name__ == "__main__":
    run()
