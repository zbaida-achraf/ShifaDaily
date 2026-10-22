"""Renders the captioned Play Store phone screenshots (FR/EN/AR) from real emulator captures.

Usage: python render_shots.py <folder with raw emulator screenshots>
Each output is 1080x1920 (9:16), 24-bit PNG, with the genuine app screen inside a device frame.
"""
import os
import shutil
import subprocess
import sys
import urllib.parse
from pathlib import Path

from PIL import Image, ImageDraw

SHOTS = Path(sys.argv[1])
ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / "src" / "work"
WORK.mkdir(parents=True, exist_ok=True)
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

# The emulator shows a debug notice ("Serial console enabled") under our notification; it is not
# app UI, so it is blanked with the shade's own background colour.
for name in ["p02-notification-en", "q02-notification-ar"]:
    im = Image.open(SHOTS / f"{name}.png").convert("RGB")
    bg = im.getpixel((540, 1700))
    ImageDraw.Draw(im).rectangle((0, 955, 1080, 1460), fill=bg)
    im.save(WORK / f"{name}.png")
for f in SHOTS.iterdir():
    if f.name[0] in "pq" and f.suffix == ".png" and "notification" not in f.name:
        shutil.copy(f, WORK / f.name)

CAPTIONS = {
    "fr": ["Votre journée en un coup d'œil", "Des rappels même sans Internet", "Des horaires de prise flexibles",
           "Des idées de repas avec ce que vous avez", "Adaptez chaque recette", "Des étapes claires et simples",
           "Guide alimentaire : à volonté, limité, occasionnel", "Vérifiez que vos rappels vous parviendront"],
    "en": ["Your day at a glance", "Reminders that work offline", "Flexible medication schedules",
           "Meal ideas from what you have", "Adapt any recipe", "Clear, simple steps",
           "Food guide: as desired, limited, occasionally", "Check that reminders can reach you"],
    "ar": ["يومك في لمحة", "تذكيرات تعمل دون إنترنت", "جداول أدوية مرنة", "أفكار وجبات مما لديك",
           "كيّف أي وصفة", "خطوات واضحة وبسيطة", "دليل غذائي: حسب الرغبة، محدود، أحيانًا", "تأكد من وصول التذكيرات إليك"],
}
ORDER_EN = ["p03-home-en", "p02-notification-en", "p04-meds-en", "p06-suggestions-en", "p07-recipe-en",
            "p08-recipe-steps-en", "p09-guide-en", "p10-check-en"]
ORDER_AR = ["q03-home-ar", "q02-notification-ar", "q04-meds-ar", "q06-suggestions-ar", "q07-recipe-ar",
            "q08-recipe-steps-ar", "q09-guide-ar", "q10-check-ar"]
SLUGS = ["home", "reminder", "medications", "meal-ideas", "recipe", "steps", "food-guide", "reminder-check"]

template = (ROOT / "src" / "shot.html").as_uri()
for lang in ["fr", "en", "ar"]:
    out = ROOT / "screenshots" / "phone" / lang
    out.mkdir(parents=True, exist_ok=True)
    order = ORDER_AR if lang == "ar" else ORDER_EN
    for i, (src, caption) in enumerate(zip(order, CAPTIONS[lang]), 1):
        url = template + "?" + urllib.parse.urlencode({"lang": lang, "t": caption, "img": (WORK / f"{src}.png").as_uri()})
        tmp = WORK / "render.png"
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                        "--allow-file-access-from-files", "--window-size=1080,1920", f"--screenshot={tmp}", url],
                       capture_output=True, timeout=90)
        dst = out / f"{i:02d}-{SLUGS[i - 1]}.png"
        Image.open(tmp).convert("RGB").crop((0, 0, 1080, 1920)).save(dst, optimize=True)
        print(lang, dst.name, Image.open(dst).size, os.path.getsize(dst) // 1024, "KB")
