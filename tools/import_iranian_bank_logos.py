#!/usr/bin/env python3
"""Import Iranian bank SVG logos as Android VectorDrawable XML resources.

Source: snapp-store/iranian-banks-react-icons, optimized/*-color.svg.
The SVGs are downloaded only during development/CI; the Android app never
accesses the network at runtime.
"""
from __future__ import annotations

import pathlib
import subprocess
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable"
BASE_URL = "https://raw.githubusercontent.com/snapp-store/iranian-banks-react-icons/main/optimized/"

# Android resource name -> source SVG filename stem.
LOGOS = {
    "bank_saderat": "saderat-color",
    "bank_mellat": "mellat-color",
    "bank_tejarat": "tejarat-color",
    "bank_melli": "melli-color",
    "bank_sepah": "sepah-color",
    "bank_keshavarzi": "keshavarzi-color",
    "bank_parsian": "parsian-color",
    "bank_maskan": "maskan-color",
    "bank_refah": "refah-color",
    "bank_eghtesad_novin": "eghtesad-novin-color",
    "bank_pasargad": "pasargad-color",
    "bank_saman": "saman-color",
    "bank_sina": "sina-color",
    "bank_post": "post-color",
    "bank_tosee_taavon": "tosee-taavon-color",
    "bank_shahr": "shahr-color",
    "bank_ayandeh": "ayandeh-color",
    "bank_sarmayeh": "sarmayeh-color",
    "bank_dey": "dey-color",
    "bank_khavar_mianeh": "khavar-mianeh-color",
    "bank_iran_zamin": "iran-zamin-color",
    "bank_karafarin": "karafarin-color",
    "bank_gardeshgari": "gardeshgari-color",
    "bank_sanat_madan": "sanat-madan-color",
    "bank_tosee_saderat": "tosee-saderat-color",
    "bank_iran_venezuela": "iran-venezuela-color",
    "bank_resalat": "resalat-color",
    "bank_iran": "iran-color",
    "bank_melal": "melall-color",
}


def convert(svg_path: pathlib.Path, xml_path: pathlib.Path) -> None:
    subprocess.run(
        ["s2v", "-p", "3", "-i", str(svg_path), "-o", str(xml_path)],
        check=True,
    )


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = ROOT / ".bank-logo-svg-tmp"
    tmp.mkdir(exist_ok=True)
    try:
        for resource_name, source_stem in LOGOS.items():
            svg_path = tmp / f"{source_stem}.svg"
            xml_path = OUT / f"{resource_name}.xml"
            url = BASE_URL + f"{source_stem}.svg"
            print(f"download {url}")
            with urllib.request.urlopen(url, timeout=30) as response:
                svg_path.write_bytes(response.read())
            convert(svg_path, xml_path)
            print(f"created {xml_path}")
    finally:
        for path in tmp.glob("*"):
            path.unlink(missing_ok=True)
        tmp.rmdir()


if __name__ == "__main__":
    main()
