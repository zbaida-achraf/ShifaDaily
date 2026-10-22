"""Renders the 1024x500 feature graphics (FR/EN/AR) and copies tablet screenshots.

Usage: python render_graphics.py <folder with raw emulator screenshots>
"""
import shutil
import subprocess
import sys
import urllib.parse
from pathlib import Path

from PIL import Image

SHOTS = Path(sys.argv[1])
ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / "src" / "work"
WORK.mkdir(parents=True, exist_ok=True)
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def render(url: str, size: str, dst: Path, box: tuple[int, int]) -> None:
    tmp = WORK / "render.png"
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                    "--allow-file-access-from-files", f"--window-size={size}", f"--screenshot={tmp}", url],
                   capture_output=True, timeout=90)
    # Feature graphic: JPEG or 24-bit PNG without alpha.
    Image.open(tmp).convert("RGB").crop((0, 0, *box)).save(dst, optimize=True)
    print(dst.relative_to(ROOT), Image.open(dst).size, dst.stat().st_size // 1024, "KB")


feature = (ROOT / "src" / "feature.html").as_uri()
for lang, shot in [("fr", "p03-home-en"), ("en", "p03-home-en"), ("ar", "q03-home-ar")]:
    url = feature + "?" + urllib.parse.urlencode({"lang": lang, "shot": (SHOTS / f"{shot}.png").as_uri()})
    render(url, "1024,500", ROOT / "graphics" / f"feature-graphic-{lang}.png", (1024, 500))

# Tablets: genuine screenshots at 9:16 (7-inch 1080x1920 @240 dpi, 10-inch 1440x2560 @280 dpi), English UI.
names = {"1-home": "01-home", "2-meds": "02-medications", "3-meals": "03-meal-ideas", "4-recipe": "04-recipe",
         "5-check": "05-reminder-check"}
for prefix, folder in [("t7", "tablet-7"), ("t10", "tablet-10")]:
    out = ROOT / "screenshots" / folder
    out.mkdir(parents=True, exist_ok=True)
    for src, name in names.items():
        im = Image.open(SHOTS / f"{prefix}-{src}.png").convert("RGB")
        dst = out / f"{name}.png"
        im.save(dst, optimize=True)
        print(dst.relative_to(ROOT), im.size, dst.stat().st_size // 1024, "KB")
