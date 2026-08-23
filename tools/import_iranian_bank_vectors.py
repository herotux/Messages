#!/usr/bin/env python3
"""Download Iranian bank SVG masters and convert them to Android VectorDrawables."""
from __future__ import annotations

import os
import pathlib
import subprocess
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
TMP = ROOT / ".tmp-bank-svg"
OUT = ROOT / "app/src/main/res/drawable"
SHA = os.environ.get("SOURCE_SHA", "master")
BASE = f"https://raw.githubusercontent.com/zegond/logos-per-banks/{SHA}/SVG%20Assets/Bank/Color/"

BANKS = {
    "bank_melli": "Melli.svg", "bank_mellat": "Mellat.svg", "bank_tejarat": "Tejarat.svg",
    "bank_saderat": "Saderat.svg", "bank_sepah": "Sepah.svg", "bank_refah": "Refah.svg",
    "bank_maskan": "Maskan.svg", "bank_keshavarzi": "Keshavarzi.svg", "bank_sanat_madan": "Sanat_Madan.svg",
    "bank_post": "Post.svg", "bank_tosee_saderat": "Tosee_Saderat.svg", "bank_tosee_taavon": "Tosee_Taavon.svg",
    "bank_parsian": "Parsian.svg", "bank_pasargad": "Pasargad.svg", "bank_karafarin": "Karafarin.svg",
    "bank_saman": "Saman.svg", "bank_eghtesad_novin": "Eghtesad_Novin.svg", "bank_sarmayeh": "Sarmayeh.svg",
    "bank_sina": "Sina.svg", "bank_mehr_iran": "Mehr_Iran.svg", "bank_shahr": "Shahr.svg",
    "bank_gardeshgari": "Gardeshgari.svg", "bank_dey": "Dey.svg", "bank_iran_zamin": "Iran_Zamin.svg",
    "bank_resalat": "Resalat.svg", "bank_melal": "Melal.svg", "bank_khavar_mianeh": "Khavar_Mianeh.svg",
    "bank_iran_venezuela": "Iran_Venezuela.svg",
}


def main() -> None:
    TMP.mkdir(exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    for output_name, source_name in BANKS.items():
        svg = TMP / source_name
        url = BASE + urllib.parse.quote(source_name)
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                svg.write_bytes(response.read())
        except urllib.error.HTTPError as exc:
            raise RuntimeError(f"Missing source SVG: {source_name} ({exc.code})") from exc
        target = OUT / f"{output_name}.xml"
        subprocess.run(["npx", "--yes", "s2v", "-i", str(svg), "-o", str(target)], check=True)
        print(f"generated {target}")

    for path in OUT.glob("bank_*.xml"):
        text = path.read_text(encoding="utf-8")
        if "<vector" not in text or "android:pathData" not in text:
            raise RuntimeError(f"Invalid VectorDrawable generated: {path}")


if __name__ == "__main__":
    main()
