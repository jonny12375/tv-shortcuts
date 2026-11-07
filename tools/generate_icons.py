#!/usr/bin/env python3
"""
Generates the launcher icon and TV banner for the hub and for every stand-in
listed in config/buttons.json. The PNGs are committed, so a normal Gradle build
doesn't need Python; rerun this after editing the button list.

    python3 tools/generate_icons.py [--font /path/to/font.ttf]

Needs Pillow.
"""
import argparse
import json
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent

DUSK, RAISED = "#1A2130", "#2E3A50"
HIGH, LOW = "#43536F", "#141A26"
TEXT, MUTED, AMBER = "#E9EDF4", "#9BA7BA", "#F2B24A"

BOLT = [(0.60, 0.0), (0.15, 0.58), (0.45, 0.58), (0.36, 1.0), (0.85, 0.38), (0.55, 0.38)]
SS = 2  # supersampling factor, for smooth edges


def find_font(explicit):
    if explicit:
        return explicit
    for pattern in ("Roboto:bold", "Noto Sans:bold", "DejaVu Sans:bold"):
        path = subprocess.run(["fc-match", "-f", "%{file}", pattern], capture_output=True, text=True).stdout
        if path:
            return path
    sys.exit("No font found; pass --font")


def make_font(path, size, weight="Bold"):
    f = ImageFont.truetype(path, size)
    try:  # variable fonts
        f.set_variation_by_name(weight)
    except Exception:
        pass
    return f


def bolt(d, cx, cy, h, fill, halo=None):
    w = h * 0.8

    def pts(scale):
        return [(cx + (x - 0.5) * w * scale, cy + (y - 0.5) * h * scale) for x, y in BOLT]

    if halo:
        d.polygon(pts(1.22), fill=halo)
    d.polygon(pts(1.0), fill=fill)


def keycap(d, box, label=None, label_font=None, badge=True, big_bolt=False):
    """A small 3D keycap. `label` is the mark; `badge` puts a bolt on its corner."""
    x0, y0, x1, y1 = box
    w = x1 - x0
    r = w // 5
    d.rounded_rectangle((x0, y0 + w * 0.05, x1, y1 + w * 0.05), r, fill=LOW)       # base edge
    d.rounded_rectangle(box, r, fill=RAISED)                                         # cap
    d.rounded_rectangle((x0 + 5, y0 + 5, x1 - 5, y1 - 5), r - 4, outline=HIGH, width=max(2, int(w * 0.02)))
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    if label:
        d.text((cx, cy + w * 0.02), label, font=label_font, fill=TEXT, anchor="mm")
    if big_bolt:
        bolt(d, cx, cy, w * 0.62, AMBER)
    if badge:
        bolt(d, x1 - w * 0.06, y0 + w * 0.06, w * 0.4, AMBER, halo=DUSK)


def icon(font_path, mark, size=256, hub=False):
    im = Image.new("RGB", (size * SS, size * SS), DUSK)
    d = ImageDraw.Draw(im)
    pad = int(size * SS * 0.2)  # keeps the cap inside the circle crop Google TV applies
    box = (pad, pad - 6 * SS, size * SS - pad, size * SS - pad - 6 * SS)
    if hub:
        keycap(d, box, badge=False, big_bolt=True)
    else:
        keycap(d, box, mark, make_font(font_path, int(size * SS * 0.27)))
    return im.resize((size, size), Image.LANCZOS)


def banner(font_path, title, subtitle, mark, hub=False, w=640, h=360):
    im = Image.new("RGB", (w * SS, h * SS), DUSK)
    d = ImageDraw.Draw(im)
    box = (48 * SS, 80 * SS, 208 * SS, 240 * SS)
    if hub:
        keycap(d, box, badge=False, big_bolt=True)
    else:
        keycap(d, box, mark, make_font(font_path, 64 * SS))
    d.text((240 * SS, 150 * SS), title, font=make_font(font_path, 46 * SS), fill=TEXT, anchor="lm")
    d.text((240 * SS, 205 * SS), subtitle, font=make_font(font_path, 22 * SS, "Regular"), fill=MUTED, anchor="lm")
    return im.resize((w, h), Image.LANCZOS)


def save(res_dir, font_path, mark, title, subtitle, hub=False):
    out = ROOT / res_dir / "res" / "drawable-nodpi"
    out.mkdir(parents=True, exist_ok=True)
    icon(font_path, mark, hub=hub).save(out / "ic_launcher.png", optimize=True)
    banner(font_path, title, subtitle, mark, hub=hub).save(out / "banner.png", optimize=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--font")
    font_path = find_font(ap.parse_args().font)

    save("hub/src/main", font_path, None, "TV Shortcuts", "Remap your remote's app buttons.", hub=True)
    for b in json.loads((ROOT / "config" / "buttons.json").read_text()):
        save(f"standin/src/{b['id']}", font_path, b["mark"], b["label"], "Shortcut. Set in TV Shortcuts.")
    print("generated icons and banners")


if __name__ == "__main__":
    main()
