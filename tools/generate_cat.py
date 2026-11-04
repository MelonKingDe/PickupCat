#!/usr/bin/env python3
"""Generates the Pickup Cat sprite sheet + icons (needs Pillow).

Run from the project root:  python3 tools/generate_cat.py
Frame size 32x44, sheet = 5 columns x 2 rows (160x88).
Frame order (index -> row-major):
 0 idle_a   1 idle_b   2 blink    3 open     4 chew_a
 5 chew_b   6 happy    7 retch_a  8 retch_b  9 relief
"""
from PIL import Image, ImageFilter
import os

W, H = 32, 44
COLS, ROWS = 5, 2

OUT = (0x3b, 0x24, 0x18, 255)
FUR = (0xf0, 0xa0, 0x40, 255)
FUR_D = (0xd1, 0x7a, 0x22, 255)
FUR_L = (0xff, 0xd0, 0x8a, 255)
WHITE = (0xff, 0xf6, 0xe6, 255)
PINK = (0xf7, 0xa1, 0xb4, 255)
EYE = (0x22, 0x22, 0x2a, 255)
IRIS = (0x6e, 0xc8, 0x5a, 255)
MOUTH = (0x7a, 0x1f, 0x2b, 255)
TONGUE = (0xee, 0x6f, 0x84, 255)
DROOL = (0x8f, 0xcf, 0x4a, 255)

SICK_MAP = {
    FUR: (0xa8, 0xc0, 0x58, 255),
    FUR_D: (0x86, 0x9c, 0x40, 255),
    FUR_L: (0xc8, 0xd8, 0x82, 255),
    WHITE: (0xe4, 0xee, 0xc4, 255),
}

TAILS = {
    0: [(22, 37), (23, 37), (24, 36), (25, 35), (26, 34), (27, 33), (28, 32), (28, 31)],
    1: [(22, 37), (23, 37), (24, 37), (25, 36), (26, 35), (27, 34), (28, 34), (29, 34)],
    2: [(22, 38), (23, 39), (24, 40), (25, 40), (26, 39), (27, 38), (28, 37), (29, 36)],
}


def mx(x):
    return 31 - x


def build_frame(tail=0, sick=False, puff=False, eyes="open", mouth="closed", blush=False):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()

    def col(c):
        return SICK_MAP.get(c, c) if sick else c

    def put(x, y, c):
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = c

    def rect(x0, y0, x1, y1, c):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                put(xx, yy, c)

    def mrect(x0, y0, x1, y1, c):
        rect(x0, y0, x1, y1, c)
        rect(mx(x1), y0, mx(x0), y1, c)

    # --- body shapes (fur) ---
    # tail first so the body overlaps its root
    path = TAILS[tail]
    for i, (x, y) in enumerate(path):
        c = FUR_D if i % 3 == 2 else FUR
        if i >= len(path) - 2:
            c = WHITE
        rect(x, y, x + 1, y + 1, col(c))
    # arms + paws
    mrect(4, 2, 7, 27, col(FUR))
    mrect(3, 0, 8, 3, col(WHITE))
    mrect(5, 1, 5, 1, col(FUR_L))
    mrect(7, 1, 7, 1, col(FUR_L))
    # shoulders
    mrect(8, 24, 10, 28, col(FUR))
    # body
    rect(11, 23, 20, 39, col(FUR))
    rect(10, 25, 21, 37, col(FUR))
    rect(14, 25, 17, 37, col(WHITE))
    rect(13, 27, 18, 35, col(WHITE))
    # hind legs + feet
    rect(10, 37, 14, 42, col(FUR))
    rect(17, 37, 21, 42, col(FUR))
    rect(10, 41, 14, 43, col(WHITE))
    rect(17, 41, 21, 43, col(WHITE))
    # head
    rect(7, 11, 24, 21, col(FUR))
    rect(8, 10, 23, 22, col(FUR))
    rect(9, 9, 22, 23, col(FUR))
    if puff:
        mrect(6, 16, 7, 20, col(FUR_L))
    # ears
    ear = {5: (9, 10), 6: (9, 11), 7: (8, 12), 8: (8, 13), 9: (8, 14)}
    for y, (a, b) in ear.items():
        rect(a, y, b, y, col(FUR))
        rect(mx(b), y, mx(a), y, col(FUR))

    # --- outline ---
    alpha = img.split()[3]
    dil = alpha.filter(ImageFilter.MaxFilter(3))
    ap, dp = alpha.load(), dil.load()
    for y in range(H):
        for x in range(W):
            if dp[x, y] > 0 and ap[x, y] == 0:
                px[x, y] = OUT

    # --- details on top ---
    # inner ears
    for (x, y) in [(10, 7), (10, 8), (11, 8), (11, 9)]:
        put(x, y, col(PINK))
        put(mx(x), y, col(PINK))
    # cheeks / stripes
    mrect(8, 17, 10, 20, col(FUR_L))
    rect(15, 10, 16, 12, col(FUR_D))
    rect(12, 10, 12, 11, col(FUR_D))
    rect(19, 10, 19, 11, col(FUR_D))
    for (x0, x1, y) in [(10, 12, 28), (19, 21, 28), (10, 12, 31), (19, 21, 31)]:
        rect(x0, y, x1, y, col(FUR_D))
    for (x, y) in [(5, 8), (5, 13), (5, 18), (26, 8), (26, 13), (26, 18)]:
        put(x, y, col(FUR_D))

    # eyes
    L, R = 11, 18
    if eyes == "open":
        for ex in (L, R):
            rect(ex, 14, ex + 2, 16, IRIS)
            rect(ex + 1, 14, ex + 1, 16, EYE)
            put(ex, 14, WHITE)
    elif eyes == "blink":
        for ex in (L, R):
            rect(ex, 15, ex + 2, 15, EYE)
    elif eyes == "happy":
        for ex in (L, R):
            put(ex, 16, EYE)
            put(ex + 1, 15, EYE)
            put(ex + 2, 16, EYE)
    elif eyes == "squint":
        for (x, y) in [(11, 14), (12, 15), (11, 16), (20, 14), (19, 15), (20, 16)]:
            put(x, y, EYE)
    # blush
    if blush:
        for (x, y) in [(9, 17), (10, 17), (21, 17), (22, 17)]:
            put(x, y, PINK)
    # nose
    rect(15, 18, 16, 18, PINK)
    # mouth
    if mouth == "closed":
        for (x, y) in [(15, 19), (16, 19), (13, 20), (14, 20), (17, 20), (18, 20)]:
            put(x, y, OUT)
    elif mouth == "smile":
        for (x, y) in [(15, 19), (16, 19), (13, 19), (14, 20), (17, 20), (18, 19)]:
            put(x, y, OUT)
    elif mouth == "open":
        rect(13, 19, 18, 23, OUT)
        rect(14, 19, 17, 22, MOUTH)
        rect(15, 21, 16, 22, TONGUE)
        put(14, 19, WHITE)
        put(17, 19, WHITE)
    elif mouth == "chew":
        rect(13, 19, 18, 22, OUT)
        rect(14, 20, 17, 21, MOUTH)
    elif mouth == "retch_mid":
        rect(13, 19, 18, 23, OUT)
        rect(14, 20, 17, 22, MOUTH)
        put(15, 23, DROOL)
    elif mouth == "retch_wide":
        rect(12, 19, 19, 24, OUT)
        rect(13, 19, 18, 23, MOUTH)
        rect(14, 22, 17, 23, TONGUE)
        rect(14, 24, 17, 25, TONGUE)
        put(15, 26, DROOL)
        put(16, 27, DROOL)
    return img


FRAMES = [
    dict(tail=0, eyes="open", mouth="closed"),                       # 0 idle_a
    dict(tail=1, eyes="open", mouth="closed"),                       # 1 idle_b
    dict(tail=0, eyes="blink", mouth="closed"),                      # 2 blink
    dict(tail=0, eyes="open", mouth="open"),                         # 3 open
    dict(tail=1, eyes="happy", mouth="chew"),                        # 4 chew_a
    dict(tail=0, eyes="happy", mouth="closed", puff=True),           # 5 chew_b
    dict(tail=2, eyes="happy", mouth="smile", blush=True),           # 6 happy
    dict(tail=1, eyes="squint", mouth="retch_mid", sick=True, puff=True),  # 7 retch_a
    dict(tail=2, eyes="squint", mouth="retch_wide", sick=True),      # 8 retch_b
    dict(tail=1, eyes="blink", mouth="closed", sick=True),           # 9 relief
]


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    tex_dir = os.path.join(root, "src", "client", "resources", "assets", "pickupcat", "textures", "gui")
    main_assets = os.path.join(root, "src", "main", "resources", "assets", "pickupcat")
    mod_dir = os.path.join(root, "modrinth")
    for d in (tex_dir, main_assets, mod_dir):
        os.makedirs(d, exist_ok=True)

    sheet = Image.new("RGBA", (W * COLS, H * ROWS), (0, 0, 0, 0))
    for i, kw in enumerate(FRAMES):
        sheet.paste(build_frame(**kw), ((i % COLS) * W, (i // COLS) * H))
    sheet.save(os.path.join(tex_dir, "cat.png"))

    # icons: cat head on a dark tile
    head = build_frame(tail=0, eyes="open", mouth="open").crop((2, 3, 30, 31))

    def icon(size):
        bg = Image.new("RGBA", (size, size), (0x2a, 0x25, 0x3d, 255))
        scale = (size * 7 // 8) // 28
        big = head.resize((28 * scale, 28 * scale), Image.NEAREST)
        bg.alpha_composite(big, ((size - big.width) // 2, (size - big.height) // 2))
        return bg

    icon(128).save(os.path.join(main_assets, "icon.png"))
    icon(512).save(os.path.join(mod_dir, "icon.png"))

    prev = sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST)
    pbg = Image.new("RGBA", prev.size, (0x4a, 0x4a, 0x58, 255))
    pbg.alpha_composite(prev)
    pbg.save(os.path.join(root, "tools", "preview.png"))
    print("done")


if __name__ == "__main__":
    main()
