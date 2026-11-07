#!/usr/bin/env python3
"""
Generates the launcher icon and TV banner for the hub and for every stand-in
listed in config/buttons.json. The PNGs are committed, so a normal Gradle build
doesn't need Python; rerun this after editing the button list:

    uv run tools/generate_icons.py [--font /path/to/font.ttf]
"""
import argparse
import json
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent

BG_TOP, BG_BOTTOM = "#212B3F", "#151B28"
FACE_TOP, FACE_BOTTOM = "#3C4C69", "#2A374F"
RIM, SIDE = "#4A5C7B", "#121723"
TEXT, MUTED, AMBER = "#EEF1F6", "#9FABBE", "#F2B24A"
WATERMARK = "#1D2739"

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
    f = ImageFont.truetype(path, int(size))
    try:  # variable fonts
        f.set_variation_by_name(weight)
    except Exception:
        pass
    return f


def vgradient(size, top, bottom):
    mask = Image.linear_gradient("L").resize(size)
    return Image.composite(Image.new("RGB", size, bottom), Image.new("RGB", size, top), mask)


def bolt_points(cx, cy, h, scale=1.0):
    w = h * 0.8
    return [(cx + (x - 0.5) * w * scale, cy + (y - 0.5) * h * scale) for x, y in BOLT]


def keycap(im, box, mark, font_path, badge=True, big_bolt=False):
    """A soft-shadowed keycap. `mark` is the initial; `badge` puts a bolt on its corner."""
    x0, y0, x1, y1 = box
    w = x1 - x0
    r = int(w * 0.22)

    shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x0, y0 + w * 0.09, x1, y1 + w * 0.09), r, fill=(0, 0, 0, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(w * 0.06))
    im.paste(shadow, (0, 0), shadow)

    d = ImageDraw.Draw(im)
    d.rounded_rectangle((x0, y0 + w * 0.05, x1, y1 + w * 0.05), r, fill=SIDE)          # the key's side
    face = vgradient((w, w), FACE_TOP, FACE_BOTTOM)
    mask = Image.new("L", (w, w), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, w - 1), r, fill=255)
    im.paste(face, (x0, y0), mask)
    d.rounded_rectangle(box, r, outline=RIM, width=max(2, int(w * 0.012)))

    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    if mark:
        size = w * (0.55 if len(mark) == 1 else 0.40)
        d.text((cx, cy + w * 0.015), mark, font=make_font(font_path, size), fill=TEXT, anchor="mm")
    if big_bolt:
        d.polygon(bolt_points(cx, cy, w * 0.62), fill=AMBER)
    if badge:
        bx, by, bh = x1 - w * 0.05, y0 + w * 0.05, w * 0.42
        d.polygon(bolt_points(bx, by, bh, 1.22), fill=SIDE)
        d.polygon(bolt_points(bx, by, bh), fill=AMBER)


def icon(font_path, mark, size=256, hub=False):
    im = vgradient((size * SS, size * SS), BG_TOP, BG_BOTTOM)
    pad = int(size * SS * 0.2)  # keeps the cap inside the circle crop Google TV applies
    cap = size * SS - 2 * pad
    box = (pad, pad - int(cap * 0.03), pad + cap, pad + cap - int(cap * 0.03))
    keycap(im, box, None if hub else mark, font_path, badge=not hub, big_bolt=hub)
    return im.resize((size, size), Image.LANCZOS)


def fit_lines(font_path, text, max_w, start, minimum, weight="Bold"):
    """Largest size that fits on one line; otherwise wrap onto two lines at the minimum."""
    probe = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    for size in range(start, minimum - 1, -2):
        f = make_font(font_path, size, weight)
        if probe.textlength(text, font=f) <= max_w:
            return f, [text]
    f = make_font(font_path, minimum, weight)
    words, lines, cur = text.split(), [], ""
    for word in words:
        trial = f"{cur} {word}".strip()
        if cur and probe.textlength(trial, font=f) > max_w:
            lines.append(cur)
            cur = word
        else:
            cur = trial
    lines.append(cur)
    return f, lines


def banner(font_path, title, subtitle, mark, hub=False, w=640, h=360):
    im = vgradient((w * SS, h * SS), BG_TOP, BG_BOTTOM)
    # a faint oversized bolt for atmosphere, bleeding off the right edge
    ImageDraw.Draw(im).polygon(bolt_points(w * SS * 0.93, h * SS * 0.56, h * SS * 1.25), fill=WATERMARK)

    cap = 190 * SS
    box = (52 * SS, (h * SS - cap) // 2, 52 * SS + cap, (h * SS - cap) // 2 + cap)
    keycap(im, box, None if hub else mark, font_path, badge=not hub, big_bolt=hub)

    d = ImageDraw.Draw(im)
    left = 286 * SS
    max_w = (w - 36) * SS - left
    title_font, lines = fit_lines(font_path, title, max_w, 46 * SS, 30 * SS)
    sub_font, _ = fit_lines(font_path, subtitle, max_w, 24 * SS, 18 * SS, "Regular")
    line_h = title_font.size * 1.18
    gap = 14 * SS
    block = line_h * len(lines) + gap + sub_font.size * 1.2
    y = (h * SS - block) / 2
    for line in lines:
        d.text((left, y), line, font=title_font, fill=TEXT, anchor="la")
        y += line_h
    d.text((left, y + gap), subtitle, font=sub_font, fill=MUTED, anchor="la")
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

    save("hub/src/main", font_path, None, "TV Shortcuts", "Remap your app buttons.", hub=True)
    for b in json.loads((ROOT / "config" / "buttons.json").read_text()):
        save(f"standin/src/{b['id']}", font_path, b["mark"], b["label"], "Configure in TV Shortcuts")
    print("generated icons and banners")


if __name__ == "__main__":
    main()
