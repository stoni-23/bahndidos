#!/usr/bin/env python3
"""Bahndidos Eco Jump & Dash — assets v1

- Cast: chroma-key Daniel's reference sprites (green/black → RGBA), NN downscale
- Parallax BG + items + pigeon: crisp PIL procedural 16-bit SNK-style

Run: /workspace/.venv-art/bin/python build_bahndidos_assets_v1.py
"""
from __future__ import annotations

import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT_BAHN = Path("/workspace/bahndidos")
REFS = Path("/workspace/bahndidos-refs")
SCRIPT_DIR = ROOT_BAHN / "_scripts"
STARGAME = Path("/workspace/stargame-assets")
PACK = STARGAME / "pack_bahndidos_v1"
ANDROID = ROOT_BAHN / "app" / "src" / "main" / "assets"
PREVIEW_DIR = ROOT_BAHN / "preview"
PROOF = PREVIEW_DIR / "proof_assets_v1.png"
PROOF_STAR = STARGAME / "preview" / "proof_bahndidos_assets_v1.png"

# --- palette (urban Berlin dusk, Neo-Geo muted) ---
SKY_TOP = (72, 68, 78, 255)
SKY_MID = (98, 86, 88, 255)
SKY_LOW = (140, 112, 98, 255)
CLOUD = (168, 158, 152, 255)
CLOUD_D = (132, 124, 120, 255)
BRICK = (118, 78, 68, 255)
BRICK_D = (88, 56, 50, 255)
BRICK_L = (138, 98, 86, 255)
WIN = (58, 72, 78, 255)
WIN_LIT = (180, 150, 90, 255)
TREE = (52, 72, 48, 255)
TREE_D = (34, 50, 32, 255)
TREE_L = (78, 98, 58, 255)
TRUNK = (78, 54, 36, 255)
PATH = (148, 132, 118, 255)
PATH_D = (118, 104, 92, 255)
PATH_L = (168, 152, 136, 255)
CURB = (96, 92, 88, 255)
RAIL = (70, 74, 78, 255)
RAIL_H = (160, 168, 176, 255)
LAMP = (48, 48, 52, 255)
LAMP_GLOW = (220, 190, 120, 180)
BENCH = (92, 64, 42, 255)
SPATI = (160, 48, 48, 255)
CABLE = (40, 40, 44, 200)

TRANSPARENT = (0, 0, 0, 0)


def ensure_dirs():
    for d in (
        PACK / "bg", PACK / "player", PACK / "enemies", PACK / "items",
        ANDROID / "bg", ANDROID / "player", ANDROID / "enemies", ANDROID / "items",
        PREVIEW_DIR, STARGAME / "preview", SCRIPT_DIR,
    ):
        d.mkdir(parents=True, exist_ok=True)


def save(im: Image.Image, *paths: Path):
    for p in paths:
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p, "PNG")
        print(f"  wrote {p} ({im.size[0]}x{im.size[1]})")


# ---------------------------------------------------------------------------
# Chroma-key + crop + NN scale
# ---------------------------------------------------------------------------

def _is_green(r, g, b, thr=55):
    # lime green screen ~ (18,187,34)
    return g > 120 and g > r + thr and g > b + thr and r < 120 and b < 120


def _is_black(r, g, b, thr=28):
    return r <= thr and g <= thr and b <= thr


def chroma_key(im: Image.Image, mode: str) -> Image.Image:
    """mode: 'green' | 'black' | 'auto'"""
    rgba = im.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    out = Image.new("RGBA", (w, h), TRANSPARENT)
    opx = out.load()

    # sample corners to auto-detect
    if mode == "auto":
        samples = [rgba.getpixel((2, 2)), rgba.getpixel((w - 3, 2)),
                   rgba.getpixel((2, h - 3)), rgba.getpixel((w - 3, h - 3))]
        greens = sum(1 for s in samples if _is_green(*s[:3]))
        blacks = sum(1 for s in samples if _is_black(*s[:3]))
        mode = "green" if greens >= blacks else "black"

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 16:
                continue
            kill = False
            if mode == "green":
                if _is_green(r, g, b):
                    kill = True
                elif g > 100 and g > r + 35 and g > b + 35:
                    # soft fringe
                    fringe = min(255, int((g - max(r, b) - 35) * 4))
                    if fringe > 180:
                        kill = True
                    elif fringe > 40:
                        na = max(0, a - fringe)
                        if na < 8:
                            kill = True
                        else:
                            # despill
                            g2 = min(g, max(r, b) + 10)
                            opx[x, y] = (r, g2, b, na)
                            continue
            else:  # black
                if _is_black(r, g, b, thr=22):
                    kill = True
                elif r < 40 and g < 40 and b < 40:
                    lum = (r + g + b) / 3
                    if lum < 18:
                        kill = True
                    elif lum < 32:
                        # soft edge into dark clothing — keep if not pure bg
                        # only kill if neighbors are also dark (handled approx)
                        kill = True
            if not kill:
                opx[x, y] = (r, g, b, a)
    return out


def crop_alpha(im: Image.Image, pad: int = 2) -> Image.Image:
    bbox = im.getbbox()
    if not bbox:
        return im
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(im.width, r + pad)
    b = min(im.height, b + pad)
    return im.crop((l, t, r, b))


def nn_height(im: Image.Image, target_h: int) -> Image.Image:
    if im.height == target_h:
        return im
    scale = target_h / im.height
    tw = max(1, int(round(im.width * scale)))
    return im.resize((tw, target_h), Image.Resampling.NEAREST)


def quantize_soft(im: Image.Image, colors: int = 48) -> Image.Image:
    """Light palette reduce while keeping alpha."""
    # Separate alpha
    a = im.split()[-1]
    rgb = im.convert("RGB")
    q = rgb.quantize(colors=colors, method=Image.Quantize.MEDIANCUT)
    rgb2 = q.convert("RGB")
    out = rgb2.convert("RGBA")
    out.putalpha(a)
    return out


def process_ref(
    src: Path,
    dst_name: str,
    *,
    mode: str,
    heights: list[int],
    dest_sub: str,
    also: list[str] | None = None,
) -> dict:
    """Returns dict of height -> Image for preview."""
    print(f"process {src.name} → {dst_name} ({mode})")
    raw = Image.open(src)
    keyed = chroma_key(raw, mode)
    # Second pass cleanup for green shadows
    if mode == "green":
        keyed = _clean_green_shadow(keyed)
    cropped = crop_alpha(keyed, pad=4)
    results = {}
    for h in heights:
        scaled = nn_height(cropped, h)
        # mild quantize for SNK crunch on large illustration sources
        if cropped.height > 400:
            scaled = quantize_soft(scaled, 56 if h >= 128 else 40)
        results[h] = scaled
        fname = dst_name if h == heights[0] else f"{Path(dst_name).stem}_{h}.png"
        # primary = first height
        if h == heights[0]:
            fname = dst_name
        paths = [PACK / dest_sub / fname, ANDROID / dest_sub / fname]
        if also and h == heights[0]:
            for alt in also:
                paths.append(PACK / dest_sub / alt)
                paths.append(ANDROID / dest_sub / alt)
        save(scaled, *paths)
        # also write sized variants for player
        if dest_sub == "player" and h != heights[0]:
            alt = f"{Path(dst_name).stem}_{h}.png"
            save(scaled, PACK / dest_sub / alt, ANDROID / dest_sub / alt)
    return results


def _clean_green_shadow(im: Image.Image) -> Image.Image:
    """Remove dark-green contact shadows that survive chroma."""
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if g > r + 20 and g > b + 20 and g < 160 and r < 80 and b < 80:
                # leftover green shadow fringe
                px[x, y] = TRANSPARENT
            elif a < 255 and g > r + 15 and g > b + 15:
                px[x, y] = TRANSPARENT
    return im


# ---------------------------------------------------------------------------
# Seamless helpers
# ---------------------------------------------------------------------------

def put(px, w, h, x, y, c):
    if 0 <= x < w and 0 <= y < h:
        px[x, y] = c


def fill_rect(px, w, h, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(h, y1)):
        for x in range(max(0, x0), min(w, x1)):
            px[x, y] = c


def hline_wrap(px, w, h, y, x0, x1, c, step=1):
    for x in range(x0, x1, step):
        put(px, w, h, x % w, y, c)


def rect_wrap(px, w, h, x0, y0, ww, hh, c):
    for dy in range(hh):
        for dx in range(ww):
            put(px, w, h, (x0 + dx) % w, y0 + dy, c)


# ---------------------------------------------------------------------------
# Background layers (seamless horizontal)
# ---------------------------------------------------------------------------

def make_bg_sky(W=512, H=192) -> Image.Image:
    im = Image.new("RGBA", (W, H), TRANSPARENT)
    px = im.load()
    for y in range(H):
        t = y / (H - 1)
        if t < 0.45:
            u = t / 0.45
            col = tuple(int(SKY_TOP[i] + (SKY_MID[i] - SKY_TOP[i]) * u) for i in range(3)) + (255,)
        else:
            u = (t - 0.45) / 0.55
            col = tuple(int(SKY_MID[i] + (SKY_LOW[i] - SKY_MID[i]) * u) for i in range(3)) + (255,)
        for x in range(W):
            px[x, y] = col
    # blocky clouds — draw once, mirror edges for seam
    clouds = [
        (40, 28, 90, 18), (160, 42, 110, 22), (300, 20, 80, 16),
        (400, 50, 95, 20), (20, 70, 70, 14), (250, 75, 100, 18),
    ]
    for cx, cy, cw, ch in clouds:
        for dy in range(ch):
            for dx in range(cw):
                # soft block cloud with dither
                nx = (cx + dx) % W
                ny = cy + dy
                if ny >= H:
                    continue
                edge = min(dx, cw - 1 - dx, dy, ch - 1 - dy)
                if edge < 1 and (dx + dy) % 2:
                    continue
                col = CLOUD if (dx + dy * 3) % 5 else CLOUD_D
                if edge >= 2:
                    px[nx, ny] = col
                elif edge == 1:
                    px[nx, ny] = CLOUD_D
    return im


def make_bg_city_far(W=512, H=192) -> Image.Image:
    im = Image.new("RGBA", (W, H), TRANSPARENT)
    px = im.load()
    # sky strip left transparent at top; buildings from mid
    buildings = [
        (0, 70, 70, 122), (70, 90, 50, 102), (120, 55, 80, 137),
        (200, 80, 60, 112), (260, 48, 90, 144), (350, 75, 55, 117),
        (405, 60, 75, 132), (480, 85, 40, 107),
    ]
    for bx, by, bw, bh in buildings:
        # brick fill
        for dy in range(bh):
            for dx in range(bw):
                x = (bx + dx) % W
                y = by + dy
                if y >= H:
                    continue
                # brick pattern
                row = dy // 4
                col = (dx + (row % 2) * 3) // 6
                if dy % 4 == 3:
                    c = BRICK_D
                elif (dx + row) % 7 == 0:
                    c = BRICK_D
                elif (col + row) % 3 == 0:
                    c = BRICK_L
                else:
                    c = BRICK
                px[x, y] = c
        # windows
        for wy in range(by + 6, by + bh - 8, 10):
            for wx in range(bx + 4, bx + bw - 4, 8):
                lit = ((wx * 3 + wy * 7) % 11) < 3
                for dy in range(4):
                    for dx in range(3):
                        put(px, W, H, (wx + dx) % W, wy + dy, WIN_LIT if lit else WIN)
        # chimney
        if bw > 50:
            chx = bx + bw - 14
            for dy in range(12):
                for dx in range(8):
                    put(px, W, H, (chx + dx) % W, by - 12 + dy, BRICK_D)
            # smoke puff
            for dy in range(6):
                for dx in range(10):
                    if (dx + dy) % 3:
                        put(px, W, H, (chx + dx - 1) % W, by - 18 + dy, CLOUD_D)
    # ground silhouette line
    for x in range(W):
        put(px, W, H, x, H - 1, BRICK_D)
    return im


def _draw_tree(px, W, H, base_x, ground_y, scale=1.0):
    tw = int(10 * scale)
    th = int(36 * scale)
    # trunk
    for dy in range(th):
        for dx in range(tw):
            put(px, W, H, (base_x + dx) % W, ground_y - th + dy, TRUNK)
    # canopy blobs
    blobs = [
        (-18, -th - 8, 28, 22), (0, -th - 18, 32, 26), (14, -th - 6, 26, 20),
        (-8, -th + 4, 24, 16),
    ]
    for ox, oy, bw, bh in blobs:
        for dy in range(int(bh * scale)):
            for dx in range(int(bw * scale)):
                # ellipse-ish
                u = (dx / (bw * scale) - 0.5) * 2
                v = (dy / (bh * scale) - 0.5) * 2
                if u * u + v * v > 1.05:
                    continue
                c = TREE_L if (dx + dy) % 5 == 0 else (TREE if (dx * 3 + dy) % 7 else TREE_D)
                put(px, W, H, (base_x + ox + dx) % W, ground_y + oy + dy, c)


def make_bg_park_mid(W=640, H=256) -> Image.Image:
    im = Image.new("RGBA", (W, H), TRANSPARENT)
    px = im.load()
    ground_y = H - 48
    # distant grass strip
    for y in range(ground_y - 10, ground_y):
        for x in range(W):
            c = TREE_L if (x + y) % 9 == 0 else TREE
            px[x, y] = (*c[:3], 220)
    # trees
    for tx, sc in [(40, 1.1), (130, 0.9), (280, 1.2), (400, 1.0), (520, 1.15), (600, 0.85)]:
        _draw_tree(px, W, H, tx, ground_y, sc)
    # tram cables (seamless)
    for y in (ground_y - 90, ground_y - 96):
        for x in range(W):
            if x % 3 != 2:
                put(px, W, H, x, y, CABLE)
    # poles
    for px0 in (80, 320, 560):
        for dy in range(ground_y - 100, ground_y):
            put(px, W, H, px0 % W, dy, LAMP)
            put(px, W, H, (px0 + 1) % W, dy, LAMP)
        # lamp head
        for dy in range(8):
            for dx in range(10):
                put(px, W, H, (px0 - 4 + dx) % W, ground_y - 108 + dy, LAMP)
        for dy in range(4):
            for dx in range(6):
                put(px, W, H, (px0 - 2 + dx) % W, ground_y - 104 + dy, LAMP_GLOW)
    # bench
    bx, by = 200, ground_y - 18
    fill_rect(px, W, H, bx, by + 8, bx + 40, by + 14, BENCH)
    fill_rect(px, W, H, bx, by, bx + 40, by + 6, BENCH)
    fill_rect(px, W, H, bx + 2, by + 14, bx + 6, by + 22, BENCH)
    fill_rect(px, W, H, bx + 34, by + 14, bx + 38, by + 22, BENCH)
    # Späti hint (small red kiosk)
    sx, sy = 450, ground_y - 40
    fill_rect(px, W, H, sx, sy, sx + 36, ground_y, SPATI)
    fill_rect(px, W, H, sx + 2, sy + 8, sx + 16, sy + 22, WIN_LIT)
    fill_rect(px, W, H, sx + 20, sy + 8, sx + 34, sy + 22, WIN)
    # awning
    fill_rect(px, W, H, sx - 2, sy - 4, sx + 38, sy + 2, (40, 40, 48, 255))
    # "SPÄTI" blocks
    for i, chx in enumerate(range(sx + 4, sx + 32, 6)):
        fill_rect(px, W, H, chx, sy + 2, chx + 4, sy + 6, (240, 220, 80, 255))
    return im


def make_bg_ground(W=640, H=256) -> Image.Image:
    """Transparent above ground strip; cobble + curb + tram rail + manhole at bottom."""
    im = Image.new("RGBA", (W, H), TRANSPARENT)
    px = im.load()
    strip_top = H - 56
    # cobble path
    for y in range(strip_top, H):
        for x in range(W):
            # cobble cells
            cx = (x // 8)
            cy = (y // 6)
            base = PATH if (cx + cy) % 2 == 0 else PATH_D
            if x % 8 == 0 or y % 6 == 0:
                c = PATH_D
            elif (x + y) % 11 == 0:
                c = PATH_L
            else:
                c = base
            px[x, y] = c
    # curb line
    for x in range(W):
        for dy in range(4):
            put(px, W, H, x, strip_top + dy, CURB)
        put(px, W, H, x, strip_top - 1, (60, 58, 56, 255))
    # tram rail (two rails)
    for rail_y in (H - 28, H - 22):
        for x in range(W):
            put(px, W, H, x, rail_y, RAIL)
            put(px, W, H, x, rail_y + 1, RAIL_H)
        # ties
        for x in range(0, W, 12):
            for dx in range(10):
                put(px, W, H, (x + dx) % W, rail_y - 2, (90, 70, 50, 255))
                put(px, W, H, (x + dx) % W, H - 22 + 3, (90, 70, 50, 255))
    # manhole
    mx, my = 180, H - 40
    for dy in range(14):
        for dx in range(18):
            u = (dx / 18 - 0.5) * 2
            v = (dy / 14 - 0.5) * 2
            if u * u + v * v <= 1.0:
                c = (70, 72, 74, 255) if (dx + dy) % 3 else (50, 52, 54, 255)
                put(px, W, H, mx + dx, my + dy, c)
    # grate lines
    for dx in range(2, 16, 3):
        for dy in range(3, 11):
            put(px, W, H, mx + dx, my + dy, (30, 32, 34, 255))
    return im


# ---------------------------------------------------------------------------
# Items + pigeon (procedural)
# ---------------------------------------------------------------------------

def make_powerbank(size=48) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    m = size // 8
    d.rounded_rectangle([m, m + 2, size - m, size - m], radius=3, fill=(18, 18, 22, 255), outline=(60, 60, 70, 255))
    # LED
    led = size // 5
    d.ellipse([size // 2 - led // 2, m + 6, size // 2 + led // 2, m + 6 + led], fill=(40, 220, 80, 255))
    d.ellipse([size // 2 - led // 4, m + 8, size // 2 + 1, m + 8 + led // 3], fill=(180, 255, 180, 255))
    # ports
    d.rectangle([size // 2 - 6, size - m - 8, size // 2 - 2, size - m - 4], fill=(80, 80, 90, 255))
    d.rectangle([size // 2 + 2, size - m - 8, size // 2 + 6, size - m - 4], fill=(80, 80, 90, 255))
    return im


def make_bier(size=40) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    # bottle
    d.rectangle([size // 2 - 6, 10, size // 2 + 6, size - 4], fill=(40, 90, 40, 255), outline=(20, 50, 20, 255))
    d.rectangle([size // 2 - 3, 4, size // 2 + 3, 12], fill=(40, 90, 40, 255))
    d.rectangle([size // 2 - 4, 2, size // 2 + 4, 5], fill=(200, 180, 60, 255))  # cap
    d.rectangle([size // 2 - 5, 16, size // 2 + 5, 28], fill=(220, 200, 80, 255))  # label
    return im


def make_kaffee(size=40) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    d.ellipse([8, 12, size - 8, size - 4], fill=(240, 240, 245, 255), outline=(80, 80, 90, 255))
    d.ellipse([12, 16, size - 12, size - 10], fill=(60, 36, 20, 255))
    d.arc([size - 14, 18, size - 2, 32], 270, 90, fill=(180, 180, 190, 255), width=3)
    # steam
    d.line([(size // 2 - 4, 6), (size // 2 - 2, 12)], fill=(200, 200, 210, 180), width=1)
    d.line([(size // 2 + 2, 4), (size // 2 + 4, 11)], fill=(200, 200, 210, 180), width=1)
    return im


def make_wlan(size=40) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    cx, cy = size // 2, size // 2 + 4
    for r, col in [(16, (40, 140, 220, 255)), (11, (60, 180, 255, 255)), (6, (120, 210, 255, 255))]:
        d.arc([cx - r, cy - r - 4, cx + r, cy + r - 4], 220, 320, fill=col, width=2)
    d.ellipse([cx - 3, cy - 2, cx + 3, cy + 4], fill=(40, 180, 255, 255))
    # hotspot box
    d.rectangle([4, size - 10, size - 4, size - 3], fill=(30, 30, 40, 255), outline=(80, 200, 255, 255))
    return im


def make_ticket(size=48) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([4, 10, size - 4, size - 10], radius=2, fill=(240, 200, 40, 255), outline=(160, 120, 20, 255))
    d.rectangle([8, 16, size - 8, 22], fill=(40, 40, 50, 255))
    # ZONE C
    d.rectangle([10, 26, 18, 34], fill=(40, 40, 50, 255))
    d.rectangle([22, 26, 30, 34], fill=(40, 40, 50, 255))
    d.rectangle([34, 26, 38, 34], fill=(40, 40, 50, 255))
    d.line([(8, size // 2), (size - 8, size // 2)], fill=(180, 140, 30, 255))
    return im


def make_hamster(size=40) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    # battery body
    d.rounded_rectangle([6, 8, size - 6, size - 6], radius=3, fill=(180, 140, 90, 255), outline=(100, 70, 40, 255))
    d.ellipse([10, 12, 22, 24], fill=(220, 180, 120, 255))  # head
    d.ellipse([12, 16, 15, 19], fill=(30, 20, 10, 255))
    d.ellipse([17, 16, 20, 19], fill=(30, 20, 10, 255))
    d.ellipse([24, 18, 34, 28], fill=(200, 160, 100, 255))  # body
    # freeze bolt
    d.polygon([(28, 10), (32, 16), (29, 16), (34, 24), (26, 15), (30, 15)], fill=(120, 220, 255, 255))
    return im


def make_prop_bench(w=64, h=32) -> Image.Image:
    im = Image.new("RGBA", (w, h), TRANSPARENT)
    d = ImageDraw.Draw(im)
    d.rectangle([4, 8, w - 4, 14], fill=BENCH)
    d.rectangle([4, 16, w - 4, 20], fill=BENCH)
    d.rectangle([6, 20, 12, h - 2], fill=BENCH)
    d.rectangle([w - 12, 20, w - 6, h - 2], fill=BENCH)
    return im


def make_prop_trash(size=40) -> Image.Image:
    im = Image.new("RGBA", (size, size), TRANSPARENT)
    d = ImageDraw.Draw(im)
    d.rectangle([10, 8, size - 10, size - 4], fill=(70, 80, 70, 255), outline=(40, 50, 40, 255))
    d.rectangle([8, 6, size - 8, 12], fill=(50, 60, 50, 255))
    d.line([(size // 2, 12), (size // 2, size - 6)], fill=(40, 50, 40, 255))
    return im


def make_pigeon(frame: int = 0) -> Image.Image:
    """2-frame flying pigeon, side view facing RIGHT, ~32–40px."""
    w, h = 40, 28
    im = Image.new("RGBA", (w, h), TRANSPARENT)
    px = im.load()
    body = (140, 140, 148, 255)
    body_d = (100, 100, 110, 255)
    wing = (120, 122, 130, 255)
    beak = (220, 160, 60, 255)
    eye = (20, 20, 24, 255)
    # body
    for dy in range(10):
        for dx in range(16):
            u = (dx / 16 - 0.5) * 2
            v = (dy / 10 - 0.5) * 2
            if u * u + v * v <= 1.1:
                put(px, w, h, 12 + dx, 10 + dy, body if dx > 4 else body_d)
    # head
    for dy in range(7):
        for dx in range(7):
            if (dx - 3) ** 2 + (dy - 3) ** 2 <= 10:
                put(px, w, h, 26 + dx, 8 + dy, body)
    put(px, w, h, 30, 10, eye)
    # beak
    put(px, w, h, 33, 11, beak)
    put(px, w, h, 34, 11, beak)
    put(px, w, h, 33, 12, beak)
    # wing
    if frame == 0:
        # wing up
        for dy in range(10):
            for dx in range(12):
                if abs(dx - 6) < 6 - dy // 2:
                    put(px, w, h, 10 + dx, 2 + dy, wing)
    else:
        # wing down
        for dy in range(10):
            for dx in range(12):
                if abs(dx - 6) < 6 - (9 - dy) // 2:
                    put(px, w, h, 10 + dx, 12 + dy, wing)
    # tail
    for i in range(6):
        put(px, w, h, 8 - i, 14 + i // 2, body_d)
        put(px, w, h, 8 - i, 15 + i // 2, body_d)
    return im


# ---------------------------------------------------------------------------
# Preview sheet
# ---------------------------------------------------------------------------

def make_preview(sprites: list[tuple[str, Image.Image]]) -> Image.Image:
    cols = 4
    cell_w, cell_h = 200, 220
    rows = math.ceil(len(sprites) / cols)
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (36, 36, 42, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (label, im) in enumerate(sprites):
        cx = (i % cols) * cell_w
        cy = (i // cols) * cell_h
        draw.rectangle([cx + 2, cy + 2, cx + cell_w - 2, cy + cell_h - 2], outline=(70, 70, 80, 255))
        # fit
        max_w, max_h = cell_w - 16, cell_h - 36
        scale = min(max_w / im.width, max_h / im.height, 3.0)
        # for large BGs, allow downscale
        tw = max(1, int(im.width * scale))
        th = max(1, int(im.height * scale))
        if tw > max_w or th > max_h:
            scale = min(max_w / im.width, max_h / im.height)
            tw = max(1, int(im.width * scale))
            th = max(1, int(im.height * scale))
        # use NEAREST for sprites, BILINEAR for huge bg preview
        method = Image.Resampling.NEAREST if max(im.size) < 400 else Image.Resampling.BILINEAR
        thumb = im.resize((tw, th), method)
        ox = cx + (cell_w - tw) // 2
        oy = cy + 8 + (max_h - th) // 2
        sheet.alpha_composite(thumb, (ox, oy))
        draw.text((cx + 8, cy + cell_h - 18), label[:28], fill=(220, 220, 230, 255))
    return sheet


def write_bg_readme():
    text = """# Bahndidos parallax BG

Seamless horizontal tiles (side-scroller). Portrait canvas is 9:16; layers scroll on X.

| File | Size | Scroll speed | Notes |
|------|------|--------------|-------|
| `bg_sky.png` | 512×192 | **0.2×** | Overcast urban dusk, blocky clouds |
| `bg_city_far.png` | 512×192 | **0.5×** | Distant brick tenements, chimneys |
| `bg_park_mid.png` | 640×256 | **1.0×** | Chestnut trees, bench, lamp, tram cables, Späti |
| `bg_ground.png` | 640×256 | **1.8×** | Cobble path + curb + tram rail + manhole; **transparent above** ground strip |

Tile by repeating on X. True RGBA PNG.
"""
    for p in (PACK / "bg" / "README.md", ANDROID / "bg" / "README.md"):
        p.write_text(text, encoding="utf-8")
        print(f"  wrote {p}")


def write_uebergabe(manifest: list[str]):
    text = """# Übergabe — Bahndidos Eco Jump & Dash Assets v1

**Für:** Thron (Physics/Engine), Kacki (Integration)  
**Von:** Art pipeline (Pillow + Daniel-Refs chroma-key)  
**Datum:** 2026-09-20  

## Prinzip

- **Player / Opa / Oma:** Daniel-Referenzsprites → Green-/Blackscreen entfernt → true RGBA → nearest-neighbor auf Game-Höhe.
- **Parallax BG, Items, Taube, Props:** Procedural 16-bit SNK/Neo-Geo (Pillow), konsistente Palette.
- Player **schaut nach RECHTS** (Side-Scroller). Opa-Walk ebenfalls RIGHT.

## Parallax (`assets/bg/`)

| Datei | Größe | Scroll |
|-------|-------|--------|
| `bg_sky.png` | 512×192 | 0.2× |
| `bg_city_far.png` | 512×192 | 0.5× |
| `bg_park_mid.png` | 640×256 | 1.0× |
| `bg_ground.png` | 640×256 | 1.8× (transparent oberhalb Bodenstreifen) |

Siehe auch `assets/bg/README.md`.

## Player (`assets/player/`)

| Datei | Größe | Notes |
|-------|-------|-------|
| `bahndidos_scooter.png` | ~128h | Zwei Bahndidos auf rotem E-Scooter, Side, **facing RIGHT** |
| `bahndidos_scooter_160.png` | ~160h | Größere Variante |

Jump/Duck/Salto-Frames: noch keine Refs → bewusst weggelassen (keine Stick-Figures).

## Enemies (`assets/enemies/`)

| Datei | Größe | Facing / Pose |
|-------|-------|---------------|
| `opa_walk.png` | ~128h | Side walk + Stock, **RIGHT** (Primary für Gameplay) |
| `opa_idle.png` | ~128h | Frontal idle |
| `opa_threat.png` | ~128h | Frontal Faust drohend |
| `oma.png` | ~128h | Frontal, Handtasche + Brille (stehendes Hindernis) |
| `pigeon_1.png` / `pigeon_2.png` | 40×28 | 2-Frame Flug, facing RIGHT |
| `taube.png` | = pigeon_1 | Alias |

## Items (`assets/items/`)

| Datei | Größe | Wirkung (Konzept) |
|-------|-------|-------------------|
| `powerbank.png` | 48×48 | Energy |
| `bier.png` | 40×40 | Turbo |
| `kaffee.png` | 40×40 | Turbo |
| `wlan.png` | 40×40 | Shield-Hotspot |
| `ticket.png` | 48×48 | Zone-C Ticket |
| `hamster.png` | 40×40 | Freeze-Battery (optional) |
| `prop_bench.png` | 64×32 | Deko |
| `prop_trash.png` | 40×40 | Deko |

## Preview

- `/workspace/bahndidos/preview/proof_assets_v1.png`
- Script: `_scripts/build_bahndidos_assets_v1.py`
- Rebuild: `/workspace/.venv-art/bin/python _scripts/build_bahndidos_assets_v1.py`

## Dateiliste

"""
    text += "\n".join(f"- `{m}`" for m in manifest)
    text += "\n"
    path = ROOT_BAHN / "UEBERGABE.md"
    path.write_text(text, encoding="utf-8")
    print(f"  wrote {path}")
    return path


def main():
    ensure_dirs()
    manifest: list[str] = []
    preview_sprites: list[tuple[str, Image.Image]] = []

    # --- Cast from refs ---
    # Player: green, facing RIGHT already
    pr = process_ref(
        REFS / "player_scooter_side.jpg",
        "bahndidos_scooter.png",
        mode="green",
        heights=[128, 160],
        dest_sub="player",
    )
    preview_sprites.append(("bahndidos_scooter", pr[128]))
    manifest += [
        "app/src/main/assets/player/bahndidos_scooter.png",
        "app/src/main/assets/player/bahndidos_scooter_160.png",
    ]

    # Opa walk side — black BG
    ow = process_ref(
        REFS / "opa_side.jpg",
        "opa_walk.png",
        mode="black",
        heights=[128, 96],
        dest_sub="enemies",
    )
    preview_sprites.append(("opa_walk", ow[128]))
    manifest.append("app/src/main/assets/enemies/opa_walk.png")
    # also write 96
    save(ow[96], PACK / "enemies" / "opa_walk_96.png", ANDROID / "enemies" / "opa_walk_96.png")
    manifest.append("app/src/main/assets/enemies/opa_walk_96.png")

    # Opa idle front — green
    oi = process_ref(
        REFS / "opa_front.jpg",
        "opa_idle.png",
        mode="green",
        heights=[128],
        dest_sub="enemies",
    )
    preview_sprites.append(("opa_idle", oi[128]))
    manifest.append("app/src/main/assets/enemies/opa_idle.png")

    # Opa threat — black (already partial alpha)
    ot = process_ref(
        REFS / "opa_front_threat.png",
        "opa_threat.png",
        mode="black",
        heights=[128],
        dest_sub="enemies",
    )
    preview_sprites.append(("opa_threat", ot[128]))
    manifest.append("app/src/main/assets/enemies/opa_threat.png")

    # Oma front — green
    om = process_ref(
        REFS / "oma_front.jpg",
        "oma.png",
        mode="green",
        heights=[128],
        dest_sub="enemies",
    )
    preview_sprites.append(("oma", om[128]))
    manifest.append("app/src/main/assets/enemies/oma.png")

    # --- Parallax ---
    print("generate parallax BG")
    sky = make_bg_sky(512, 192)
    city = make_bg_city_far(512, 192)
    park = make_bg_park_mid(640, 256)
    ground = make_bg_ground(640, 256)
    for name, im in [
        ("bg_sky.png", sky),
        ("bg_city_far.png", city),
        ("bg_park_mid.png", park),
        ("bg_ground.png", ground),
    ]:
        save(im, PACK / "bg" / name, ANDROID / "bg" / name)
        manifest.append(f"app/src/main/assets/bg/{name}")
        preview_sprites.append((name, im))
    write_bg_readme()
    manifest.append("app/src/main/assets/bg/README.md")

    # --- Items ---
    print("generate items + pigeon")
    items = [
        ("powerbank.png", make_powerbank(48)),
        ("bier.png", make_bier(40)),
        ("kaffee.png", make_kaffee(40)),
        ("wlan.png", make_wlan(40)),
        ("ticket.png", make_ticket(48)),
        ("hamster.png", make_hamster(40)),
        ("prop_bench.png", make_prop_bench()),
        ("prop_trash.png", make_prop_trash()),
    ]
    for name, im in items:
        save(im, PACK / "items" / name, ANDROID / "items" / name)
        manifest.append(f"app/src/main/assets/items/{name}")
        preview_sprites.append((name, im))

    p1 = make_pigeon(0)
    p2 = make_pigeon(1)
    save(p1, PACK / "enemies" / "pigeon_1.png", ANDROID / "enemies" / "pigeon_1.png",
         PACK / "enemies" / "taube.png", ANDROID / "enemies" / "taube.png")
    save(p2, PACK / "enemies" / "pigeon_2.png", ANDROID / "enemies" / "pigeon_2.png")
    manifest += [
        "app/src/main/assets/enemies/pigeon_1.png",
        "app/src/main/assets/enemies/pigeon_2.png",
        "app/src/main/assets/enemies/taube.png",
    ]
    preview_sprites.append(("pigeon_1", p1))
    preview_sprites.append(("pigeon_2", p2))

    # --- Preview ---
    print("preview sheet")
    sheet = make_preview(preview_sprites)
    save(sheet, PROOF, PROOF_STAR)
    manifest.append("preview/proof_assets_v1.png")

    # copy script into stargame-assets too
    dest_script = STARGAME / "_scripts" / "build_bahndidos_assets_v1.py"
    shutil.copy2(SCRIPT_DIR / "build_bahndidos_assets_v1.py", dest_script)
    print(f"  copied script → {dest_script}")

    write_uebergabe(manifest)
    print("DONE", len(manifest), "files")


if __name__ == "__main__":
    # Ensure script path exists when run after write
    main()
