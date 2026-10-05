"""Makes the French menu images of RealRTCW from its English ones.

The menu buttons are 2000x2000 images with the text drawn in: this erases it and draws the French
text from images_fr.py at the same place. Titles (capitals) use the game's own font atlas, the
help lines Bahnschrift (Windows), close to the font of the originals.

    python make_images.py <RealRTCW main folder> [output folder]

It reads z_realrtcw_localization.pk3 (the English images and the font) and writes the French images
to ../fr/ui/assets by default.
"""
import io
import os
import struct
import sys
import zipfile

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from images_fr import IMAGES  # noqa: E402

HINT_FONT = r'C:\Windows\Fonts\bahnschrift.ttf'
SKULL = 368  # width of the skull picture at the right of the banners


class Atlas:
    """The game's font (fontImage_24.dat + its two 1024x1024 pages)."""

    def __init__(self, pk3):
        d = pk3.read('fonts/fontImage_24.dat')
        self.pages = {}
        self.glyphs = {}
        for c in range(256):
            h, top, bottom, pitch, xskip, iw, ih, s, t, s2, t2, _, name = struct.unpack_from('<7i4fi32s', d, c * 80)
            name = name.split(b'\0')[0].decode()
            if iw <= 0 or not name:
                continue
            if name not in self.pages:
                self.pages[name] = Image.open(io.BytesIO(pk3.read(name))).convert('RGBA')
            pg = self.pages[name]
            box = (round(s * pg.width), round(t * pg.height), round(s2 * pg.width), round(t2 * pg.height))
            self.glyphs[c] = (pg.crop(box), top, xskip, pitch)

    def has(self, text):
        return all(ord(ch) in self.glyphs or ch == ' ' for ch in text)

    def render(self, text, cap):
        """Text as an alpha mask with its capitals 'cap' pixels high; returns (mask, baseline)."""
        scale = cap / self.glyphs[ord('A')][1]
        ups = max(g[1] for g in self.glyphs.values())
        w = sum(self.glyphs[ord(ch)][2] if ord(ch) in self.glyphs else self.glyphs[ord('A')][2] // 2 for ch in text) + 8
        h = ups + 40
        img = Image.new('L', (w, h), 0)
        x = 0
        for ch in text:
            g = self.glyphs.get(ord(ch))
            if g is None:
                x += self.glyphs[ord('A')][2] // 2
                continue
            im, top, xskip, pitch = g
            img.paste(im.getchannel('A'), (x + pitch // 2, ups - top), im.getchannel('A'))
            x += xskip
        img = img.crop((0, 0, max(x, 1), h))
        img = img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))), Image.LANCZOS)
        return img, round(ups * scale)


def hint_font(size):
    f = ImageFont.truetype(HINT_FONT, size)
    f.set_variation_by_name('Bold SemiCondensed')
    return f


def render_hint(text, size):
    f = hint_font(size)
    x0, y0, x1, y1 = f.getbbox(text, anchor='ls')
    img = Image.new('L', (x1 - x0 + 4, y1 - y0 + 4), 0)
    ImageDraw.Draw(img).text((2 - x0, 2 - y0), text, font=f, fill=255, anchor='ls')
    return img, 2 - y0


def hint_size(english, height):
    """The font size whose rendering of the English line is as high as the original."""
    lo, hi = 8, 200
    while lo < hi:
        mid = (lo + hi + 1) // 2
        x0, y0, x1, y1 = hint_font(mid).getbbox(english, anchor='ls')
        if y1 - y0 <= height:
            lo = mid
        else:
            hi = mid - 1
    return lo, -hint_font(lo).getbbox(english, anchor='ls')[1]


def paste(dst, mask, x, top, color, max_w=None):
    if max_w and mask.width > max_w:
        mask = mask.resize((max_w, mask.height), Image.LANCZOS)
    layer = Image.new('RGBA', mask.size, tuple(int(c) for c in color) + (255,))
    layer.putalpha(mask)
    dst.alpha_composite(layer, (int(x), int(top)))
    return mask.width


def after_widest_gap(cols):
    """First column after the widest gap: where the title starts after "Mission N"."""
    return int(cols[np.argmax(np.diff(cols)) + 1])


def text_lines(a):
    """Rows of light text on transparency: [(x0, y0, x1, y1)]"""
    rr = np.where((a > 60).sum(axis=1) > 0)[0]
    out = []
    if len(rr):
        for run in np.split(rr, np.where(np.diff(rr) > 6)[0] + 1):
            ys, ye = run[0], run[-1]
            cs = np.where((a[ys:ye + 1] > 60).sum(axis=0) > 0)[0]
            out.append((int(cs[0]), int(ys), int(cs[-1]), int(ye)))
    return out


def find_banner(a):
    rows = (a > 200).sum(axis=1)
    br = np.where(rows > 500)[0]
    if not len(br):
        return None
    r = max(np.split(br, np.where(np.diff(br) > 1)[0] + 1), key=len)
    if len(r) < 30:
        return None
    y0, y1 = int(r[0]), int(r[-1])
    cols = np.where((a[y0:y1 + 1] > 200).sum(axis=0) > (y1 - y0) // 2)[0]
    return int(cols[0]), y0, int(cols[-1]), y1


def cap_font_size(cap):
    """The hint font size whose capitals are 'cap' pixels high."""
    lo, hi = 8, 300
    while lo < hi:
        mid = (lo + hi + 1) // 2
        x0, y0, x1, y1 = hint_font(mid).getbbox('H', anchor='ls')
        if -y0 <= cap:
            lo = mid
        else:
            hi = mid - 1
    return lo


def draw_text(img, text, english, box, color, atlas, max_w=None, caps_line=None):
    """French text at the place of the English one (box: its bounding box; caps_line: the top and
    the baseline of its capitals, when known)."""
    x0, y0, x1, y1 = box
    caps = english.upper() == english and atlas.has(text)
    if caps:
        cap = (y1 - y0) * (0.82 if any(c in english for c in 'QJ,;') else 1.0)
        mask, base = atlas.render(text, cap)
        return paste(img, mask, x0, y0 + cap - base, color, max_w)
    if caps_line:
        top, baseline = caps_line
        mask, base = render_hint(text, cap_font_size(baseline - top + 1))
        return paste(img, mask, x0 - 2, baseline + 1 - base, color, max_w)
    size, ascent = hint_size(english, y1 - y0 + 1)
    mask, base = render_hint(text, size)
    return paste(img, mask, x0 - 2, y0 + ascent - base, color, max_w)


def first_letter_rows(mask, x0):
    """Top and bottom rows of the first letter at or after column x0 of a text mask (its capital)."""
    cols = np.where(mask[:, x0:].sum(axis=0) > 0)[0] + x0
    first = np.split(cols, np.where(np.diff(cols) > 2)[0] + 1)[0]
    rows = np.where(mask[:, first[0]:first[-1] + 1].sum(axis=1) > 0)[0]
    return int(rows[0]), int(rows[-1])


def inpaint(img, mask_box, mask):
    """Rebuilds the pixels under 'mask' (a boolean array over mask_box) from their surroundings."""
    x0, y0, x1, y1 = mask_box
    region = np.array(img.crop((x0, y0, x1, y1)))
    m = cv2.dilate(mask.astype(np.uint8) * 255, np.ones((9, 9), np.uint8))
    rgb = cv2.inpaint(np.ascontiguousarray(region[:, :, :3]), m, 9, cv2.INPAINT_TELEA)
    region[:, :, :3] = rgb
    img.paste(Image.fromarray(region), (x0, y0))


def make(pk3, atlas, name, spec, out_dir):
    src = Image.open(io.BytesIO(pk3.read('ui/assets/%s.png' % name))).convert('RGBA')
    arr = np.array(src).astype(int)
    a = arr[:, :, 3]
    img = src.copy()
    banner = find_banner(a)

    if banner and spec.get('banner'):
        bx0, by0, bx1, by1 = banner
        sub = arr[by0:by1 + 1, bx0:bx1 + 1]
        lum = sub[:, :, :3].mean(axis=2)
        # each row's colour, from the clean strip at the banner's left end (the text starts ~30 px in)
        row_bg = np.median(sub[:, 6:22, :3], axis=1)
        diff = (np.abs(lum - row_bg.mean(axis=1)[:, None]) > 70) & (sub[:, :, 3] > 200)
        diff[:6, :] = False
        diff[-6:, :] = False
        diff[:, :22] = False
        cols = np.where(diff.sum(axis=0) > 0)[0]
        # text, then the skull picture: the same graphic on every banner, SKULL px from its right end
        skull_x = (bx1 - bx0) - SKULL
        dark = (lum < 80) & (sub[:, :, 3] > 200)
        dark[:6, :] = False
        dark[-6:, :] = False
        dark[:, :22] = False
        if dark.sum() > 200:  # dark text: the skull has no pixel that dark, the text can run onto it
            diff = dark
            text_cols = np.where(dark.sum(axis=0) > 0)[0]
            light = False
        else:  # light text (red / green banners)
            text_cols = cols[cols < skull_x - 4]
            light = True
        tx0, tx1 = bx0 + int(text_cols[0]), bx0 + int(text_cols[-1])
        rows = np.where(diff[:, text_cols[0]:text_cols[-1] + 1].sum(axis=1) > 0)[0]
        ty0, ty1 = by0 + int(rows[0]), by0 + int(rows[-1])
        tmask = np.zeros_like(diff)
        tmask[:, text_cols[0]:text_cols[-1] + 1] = diff[:, text_cols[0]:text_cols[-1] + 1]
        color = sub[:, :, :3][tmask].mean(axis=0).astype(int)
        english, french = spec['banner']
        if spec.get('keep_prefix'):  # "Mission N" stays, only the title after it changes
            tx0 = after_widest_gap(text_cols + bx0)
            rows = np.where(diff[:, tx0 - bx0:text_cols[-1] + 1].sum(axis=1) > 0)[0]
            ty0, ty1 = by0 + int(rows[0]), by0 + int(rows[-1])
        text_end = bx0 + int(text_cols[-1])
        room_end = max(bx0 + skull_x + 30, text_end)  # as the originals, the text may run onto the skull's faded edge
        if ty1 - ty0 > 60:  # caught the banner's edges: the text of the other banners is 49 px high, 31 px down
            ty0, ty1 = by0 + 31, by0 + 31 + 49
        # erase: the letters rebuilt from the banner around them
        erase = np.zeros_like(diff)
        erase[:, tx0 - bx0 - 4:text_cols[-1] + 5] = diff[:, tx0 - bx0 - 4:text_cols[-1] + 5]
        inpaint(img, (bx0, by0, bx1 + 1, by1 + 1), erase)
        caps_line = None
        if spec.get('keep_prefix'):  # level with "Mission N"
            top, bottom = first_letter_rows(diff, tx0 - bx0)
            caps_line = (by0 + top, by0 + bottom)
        draw_text(img, french, english, (tx0, ty0, tx1, ty1), color, atlas, max_w=room_end - tx0, caps_line=caps_line)

    # light text on transparency, outside the banner
    work = a.copy()
    if banner:
        bx0, by0, bx1, by1 = banner
        work[max(0, by0 - 25):by1 + 26, max(0, bx0 - 25):bx1 + 26] = 0
    lines = [l for l in text_lines(work) if l[3] - l[1] >= 20]
    texts = spec.get('lines', [])
    assert len(texts) == len(lines), (name, len(texts), len(lines), lines)
    for (x0, y0, x1, y1), item in zip(lines, texts):
        if item is None:
            continue
        english, french = item
        reg = arr[y0:y1 + 1, x0:x1 + 1]
        m = reg[:, :, 3] > (128 if (reg[:, :, 3] > 128).any() else 0)
        color = reg[:, :, :3][m].mean(axis=0).astype(int)
        if spec.get('keep_prefix'):
            if english != 'BACK':
                x0 = after_widest_gap(np.where((a[y0:y1 + 1, x0:x1 + 1] > 60).sum(axis=0) > 0)[0] + x0)
                hs = np.where((a[y0:y1 + 1, x0:x1 + 1] > 60).sum(axis=1) > 0)[0]
                y0, y1 = y0 + hs[0], y0 + hs[-1]
        clear = np.array(img)
        clear[y0 - 6:y1 + 7, x0 - 6:x1 + 7, 3] = 0
        img = Image.fromarray(clear)
        draw_text(img, french, english, (x0, y0, x1, y1), color, atlas, max_w=1960 - x0)

    os.makedirs(out_dir, exist_ok=True)
    img.save(os.path.join(out_dir, name + '.png'), optimize=True)


def main():
    main_dir = sys.argv[1]
    out_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', 'fr', 'ui', 'assets')
    pk3 = zipfile.ZipFile(os.path.join(main_dir, 'z_realrtcw_localization.pk3'))
    atlas = Atlas(pk3)
    for name, spec in IMAGES.items():
        make(pk3, atlas, name, spec, out_dir)
        print(name)


if __name__ == '__main__':
    main()
