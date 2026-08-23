#!/usr/bin/env python3
"""Import 64px Iranian bank logos into Android resources.

Development-time only: the app never accesses the network. The generated PNG
files are committed into the APK resources.

Source: amastaneh/IranianBankLogos, src/ibls64.png.
"""
from __future__ import annotations

import io
import pathlib
import urllib.request

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable"
SPRITE_URL = "https://raw.githubusercontent.com/amastaneh/IranianBankLogos/master/src/ibls64.png"

# Exact 64px positions from IranianBankLogos/src/ibl.css.
LOGOS = {
    "bank_saderat": (0, 0),
    "bank_mellat": (1, 0),
    "bank_tejarat": (2, 0),
    "bank_melli": (3, 0),
    "bank_sepah": (4, 0),
    "bank_keshavarzi": (0, 1),
    "bank_parsian": (1, 1),
    "bank_maskan": (2, 1),
    "bank_refah": (3, 1),
    "bank_eghtesad_novin": (4, 1),
    "bank_sepah_ansar_legacy": (0, 2),
    "bank_pasargad": (1, 2),
    "bank_saman": (2, 2),
    "bank_sina": (3, 2),
    "bank_post": (4, 2),
    "bank_ghavamin_legacy": (0, 3),
    "bank_tosee_taavon": (1, 3),
    "bank_shahr": (2, 3),
    "bank_ayandeh_legacy": (3, 3),
    "bank_sarmayeh": (4, 3),
    "bank_dey": (0, 4),
    "bank_khavar_mianeh": (1, 4),
    "bank_iran_zamin": (2, 4),
    "bank_karafarin": (3, 4),
    "bank_gardeshgari": (4, 4),
    "bank_sanat_madan": (0, 5),
    "bank_tosee_saderat": (1, 5),
    "bank_khavar_mianeh_alt": (2, 5),
    "bank_iran_venezuela": (3, 5),
    "bank_resalat": (4, 5),
    "bank_iran": (0, 6),
    "bank_melal": (1, 6),
    "bank_refah_alt": (2, 6),
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(SPRITE_URL, timeout=30) as response:
        sprite = Image.open(io.BytesIO(response.read())).convert("RGBA")

    if sprite.width < 320 or sprite.height < 448:
        raise RuntimeError(f"Unexpected sprite size: {sprite.size}")

    for name, (column, row) in LOGOS.items():
        left, top = column * 64, row * 64
        sprite.crop((left, top, left + 64, top + 64)).save(
            OUT / f"{name}.png", optimize=True
        )
        print(f"created {name}.png")


if __name__ == "__main__":
    main()
