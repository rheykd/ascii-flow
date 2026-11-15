"""Regenerate the bundled UI font (optional, requires fontTools and Pillow)."""
from pathlib import Path
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw, ImageFont

RES = Path(__file__).resolve().parents[1] / 'app/src/main'
ASSETS = RES / 'assets/fonts'
bitmap = (ASSETS / 'terminal8x8.bin').read_bytes()
cousine = ImageFont.truetype(str(ASSETS / 'Cousine-Regular.ttf'), 9)
with TTFont(ASSETS / 'Cousine-Regular.ttf') as original:
    source_chars = original.getBestCmap()
codes = set(range(32, 127)) | set(range(0x400, 0x460))
codes.update(map(ord, '×≈◎◉✦●·ϟ♣✿∧▥♧❄−–—«»ёЁ '))
glyphs = {'.notdef': TTGlyphPen(None).glyph()}
cmap = {}
for code in sorted(codes):
    name = f'uni{code:04X}'
    if code < 127:
        rows = bitmap[code * 8:code * 8 + 8]
    else:
        if code not in source_chars:
            continue
        image = Image.new('L', (8, 11))
        ImageDraw.Draw(image).text((1, 8), chr(code), anchor='ls', font=cousine, fill=255)
        rows = [sum((image.getpixel((x, y)) >= 95) << x for x in range(8)) for y in range(11)]
    pen = TTGlyphPen(None)
    for row, bits in enumerate(rows):
        for col in range(8):
            if bits & (1 << col):
                x = col * 72
                y = (6 - row if code < 127 else 7 - row) * 100
                pen.moveTo((x, y))
                pen.lineTo((x, y + 100))
                pen.lineTo((x + 72, y + 100))
                pen.lineTo((x + 72, y))
                pen.closePath()
    cmap[code] = name
    glyphs[name] = pen.glyph()

font = FontBuilder(1000, isTTF=True)
font.setupGlyphOrder(list(glyphs))
font.setupCharacterMap(cmap)
font.setupGlyf(glyphs)
font.setupHorizontalMetrics({name: (600, getattr(glyph, 'xMin', 0)) for name, glyph in glyphs.items()})
font.setupHorizontalHeader(ascent=820, descent=-310)
font.setupNameTable({
    'familyName': 'ASCII Flow Terminal UI',
    'styleName': 'Regular',
    'uniqueFontIdentifier': 'ASCIIFlowTerminalUI-Regular',
    'fullName': 'ASCII Flow Terminal UI Regular',
    'psName': 'ASCIIFlowTerminalUI-Regular',
    'copyright': 'ASCII bitmap: Daniel Hepper, public domain. Supplement derived from Cousine, Copyright 2026 The Cousine Project Authors.',
    'licenseDescription': 'SIL Open Font License 1.1',
    'licenseInfoURL': 'https://openfontlicense.org',
})
font.setupOS2(sTypoAscender=820, sTypoDescender=-310, usWinAscent=820, usWinDescent=310)
font.setupPost(isFixedPitch=1)
output = RES / 'res/font/terminal_ui.ttf'
output.parent.mkdir(parents=True, exist_ok=True)
font.save(output)
