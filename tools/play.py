"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/<name>.jpg (1080x2341 renders of the demo, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen runs off the bottom edge under
its caption. Everything sits on the device's dark case, with the radar's rings behind the words.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONT = ROOT / "app/src/main/res/font/m6x11.ttf"

# The device's colours (ui/Theme.kt): its case, the paper of the screen, the red of a near trace.
CASE = (0x2B, 0x2D, 0x2A)
CASE_L = (0x3C, 0x3F, 0x3B)
CASE_D = (0x16, 0x17, 0x15)
PAPER = (0xE3, 0xE0, 0xD4)
MUTED = (0x9A, 0x9C, 0x94)
RED = (0xD6, 0x49, 0x3F)

# The order they appear in the store and the caption: a line in paper and one in red.
PAGES = [
    ("3-capture", ("A trail camera catches", "what came to the rite"), ("Фотоловушка ловит того,", "кто пришёл на обряд")),
    ("1-scan", ("The scanner hears traces", "at real places around you"), ("Сканер слышит следы", "в настоящих местах вокруг")),
    ("2-rite", ("Each rite is checked", "by GPS, compass and sensors"), ("Каждый обряд проверяют", "GPS, компас и датчики")),
    ("4-bestiary", ("50 creatures:", "bridges, springs, stairs"), ("50 существ:", "мосты, родники, лестницы")),
    ("5-card", ("Fog, rain and frost", "show in every frame"), ("Туман, дождь и мороз", "видны на каждом снимке")),
    ("7-rumours", ("Every catch tells", "of someone else"), ("Каждый пойманный", "расскажет о другом")),
    ("e-episode", ("A story for three years", "opens as you walk"), ("История на три года", "открывается в пути")),
    ("d2-bestiary", ("A night mode", "for the dark streets"), ("Ночной режим", "для тёмных улиц")),
]


def pixel(size):
    return ImageFont.truetype(str(FONT), size)


def fit(text, size, width):
    # m6x11 is drawn on a 16 px grid: keep to its multiples, so the pixels stay square.
    while size > 16 and pixel(size).getlength(text) > width:
        size -= 16
    return pixel(size)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=36, outline=CASE_L):
    """A screen on the case: rounded corners, a soft shadow and a thin edge."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), r, fill=CASE_D + (200,))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(24)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), r, outline=outline, width=3)


def board(W, H, cx, cy, rad):
    """The dark case with the radar's rings and a sweep, faint, around (cx, cy)."""
    g = Image.new("RGBA", (W, H), CASE + (255,))
    d = ImageDraw.Draw(g)
    for k in (1.0, .66, .33):
        r = rad * k
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=CASE_L, width=3)
    d.line((cx - rad, cy, cx + rad, cy), fill=CASE_L, width=2)
    d.line((cx, cy - rad, cx, cy + rad), fill=CASE_L, width=2)
    # a scan-line grain, like the device's screen
    lines = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lines)
    for y in range(0, H, 6):
        ld.line((0, y, W, y), fill=(0, 0, 0, 28))
    g.alpha_composite(lines)
    return g


def blip(g, x, y, s=14):
    ImageDraw.Draw(g).rectangle((x - s // 2, y - s // 2, x + s // 2, y + s // 2), fill=RED)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = board(W, H, W - 160, 150, 300)
    blip(g, W - 250, 90)
    d = ImageDraw.Draw(g)
    d.text((W / 2, 140), caption[0], font=fit(caption[0], 80, W - 100), fill=PAPER, anchor="mm")
    d.text((W / 2, 240), caption[1], font=fit(caption[1], 64, W - 100), fill=RED, anchor="mm")
    im = Image.open(src).convert("RGB")
    w = 880
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 330
    # Only the top corners show: the screen runs on past the bottom edge.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = board(W, H, 300, 250, 420)
    d = ImageDraw.Draw(g)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((112, 112), Image.LANCZOS)
    g.alpha_composite(logo, (56, 64))
    lines = ("Creatures wait", "at real places in your city") if lang == "en" else ("Существа ждут", "в настоящих местах города")
    # The IR frame of the catch, cut from its screen; the words keep to the left of it.
    im = Image.open(SHOTS / lang / "3-capture.jpg").convert("RGB").crop((73, 314, 1007, 1012))
    im = im.resize((round(im.width * 340 / im.height), 340), Image.LANCZOS)
    left = W - im.width - 48
    name = "Fieldscan" if lang == "en" else "Сканер"
    d.text((190, 120), name, font=fit(name, 80, left - 190 - 24), fill=PAPER, anchor="ls")
    d.text((193, 166), "FIELDSCAN-7", font=pixel(32), fill=MUTED, anchor="ls")
    d.rectangle((60, 232, 260, 237), fill=RED)
    f = min((fit(l, 48, left - 60 - 24) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        d.text((58, 270 + i * 58), l, font=f, fill=PAPER if i == 0 else RED)
    lay(g, im, left, 80, r=20, outline=MUTED)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Play rounds the icon itself: the logo goes on a full square of its own background, no transparent corners.
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((512, 512), Image.LANCZOS)
    square = Image.new("RGBA", (512, 512), logo.getpixel((256, 256 - 200))[:3] + (255,))
    square.alpha_composite(logo)
    square.convert("RGB").save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
