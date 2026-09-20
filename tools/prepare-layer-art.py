"""Derive runtime midground layer assets for the alpha13 scene (0.7.0; 1.4.0 七主题铺开).

Resizes the cleaned source PNG to the runtime budget (810x1440), re-clears
resize-induced haze, and writes a lossy WebP with alpha — soft decorative
edges compress to ~1/4 of PNG at imperceptible quality loss (drawn ≤0.55 opacity).
Re-runnable; original sources stay untouched in images/Lolita/.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "lolita"
OUT.mkdir(parents=True, exist_ok=True)

SIZE = (810, 1440)
HAZE_THRESHOLD = 8
WEBP_QUALITY = 80

JOBS = [
    ("images/Lolita/中景装饰层-clean.png", "night_midground.webp"),
    ("images/Lolita/七主题中景/晨雾蓝瓷.png", "morning_midground.webp"),
    ("images/Lolita/七主题中景/午后藕荷.png", "afternoon_midground.webp"),
    ("images/Lolita/七主题中景/黄昏奶茶.png", "twilight_midground.webp"),
    ("images/Lolita/七主题中景/雾紫玫瑰.png", "lavender_midground.webp"),
    ("images/Lolita/七主题中景/黑哥特.png", "gothic_midground.webp"),
    ("images/Lolita/七主题中景/白圣职.png", "cleric_midground.webp"),
    ("images/Lolita/七主题中景/薄荷巧克力.png", "thin_mint_midground.webp"),
]


def run() -> None:
    for source, output in JOBS:
        src_path = ROOT / source
        img = Image.open(src_path).convert("RGBA")
        img = img.resize(SIZE, Image.LANCZOS)
        alpha = img.getchannel("A")
        img.putalpha(alpha.point(lambda v: 0 if 0 < v < HAZE_THRESHOLD else v))
        dst_path = OUT / output
        img.save(dst_path, "WEBP", quality=WEBP_QUALITY, method=6)
        size_kb = dst_path.stat().st_size / 1024
        print(f"{source} -> {dst_path.name}: {SIZE[0]}x{SIZE[1]}, {size_kb:.0f} KB, webp q{WEBP_QUALITY}, haze-cleared < {HAZE_THRESHOLD}")


if __name__ == "__main__":
    run()

