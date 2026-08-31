#!/usr/bin/env python3
"""Import Iranian bank SVG logos as Android VectorDrawable XML resources.

Sepah is intentionally kept as a PNG resource because its Android vector
conversion has caused compatibility/rendering problems in the app.
"""

from __future__ import annotations

import pathlib
import subprocess
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable"
LOCAL = ROOT / "tools/bank-logo-sources"
BASE_URL = "https://raw.githubusercontent.com/snapp-store/iranian-banks-react-icons/main/optimized/"

# Android resource name -> verified upstream SVG filename.
LOGOS = {
    "bank_saderat": "saderat-color.svg",
    "bank_mellat": "mellat-color.svg",

    # These are bundled locally from the SVG files supplied by the user.
    "bank_tejarat": "bank_tejarat.svg",
    "bank_melli": "bank_melli.svg",
    # bank_sepah is intentionally excluded: the app uses bank_sepah.png.

    "bank_keshavarzi": "keshavarzi-color.svg",
    "bank_parsian": "parsian-color.svg",
    "bank_maskan": "maskan-color.svg",
    "bank_refah": "refah-color.svg",
    "bank_eghtesad_novin": "eghtesad-novin-color.svg",
    "bank_pasargad": "pasargad-color.svg",
    "bank_saman": "saman-color.svg",
    "bank_sina": "sina-color.svg",
    "bank_post": "post-color.svg",
    "bank_tosee_taavon": "tosee-taavon-color.svg",
    "bank_shahr": "shahr-color.svg",
    "bank_ayandeh": "ayandeh-color.svg",
    "bank_sarmayeh": "sarmayeh-color.svg",
    "bank_dey": "dey-color.svg",
    "bank_khavar_mianeh": "khavar-mianeh-color.svg",
    "bank_iran_zamin": "iran-zamin-color.svg",
    "bank_karafarin": "karafarin-color.svg",
    "bank_gardeshgari": "gardeshgari-color.svg",
    "bank_sanat_madan": "sanat-madan-color.svg",
    "bank_tosee_saderat": "tosee-saderat-color.svg",
    "bank_iran_venezuela": "iran-venezuela-color.svg",
    "bank_resalat": "resalat-color.svg",
    "bank_melal": "melall-color.svg",
}

LOCAL_LOGOS = {
    "bank_tejarat",
    "bank_melli",
}


def fetch(url: str, dest: pathlib.Path) -> None:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "Messages-bank-logo-import/1.0"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        data = response.read()

    if b"<svg" not in data.lower():
        raise RuntimeError(f"Downloaded file is not SVG: {url}")

    dest.write_bytes(data)


def convert(svg_path: pathlib.Path, xml_path: pathlib.Path) -> None:
    subprocess.run(
        [
            "npx",
            "--yes",
            "--package",
            "svg2vectordrawable@2.9.1",
            "s2v",
            "-p",
            "3",
            "-i",
            str(svg_path),
            "-o",
            str(xml_path),
        ],
        check=True,
    )


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)

    # Never allow the old/problematic Sepah vector resource to survive a run.
    sepah_xml = OUT / "bank_sepah.xml"
    sepah_xml.unlink(missing_ok=True)

    # The PNG is the authoritative Sepah logo resource.
    sepah_png = OUT / "bank_sepah.png"
    if not sepah_png.is_file():
        raise RuntimeError(f"Required Sepah PNG is missing: {sepah_png}")

    tmp = ROOT / ".bank-logo-svg-tmp"
    tmp.mkdir(exist_ok=True)

    try:
        for resource_name, filename in LOGOS.items():
            svg_path = tmp / filename
            xml_path = OUT / f"{resource_name}.xml"

            if resource_name in LOCAL_LOGOS:
                source = LOCAL / filename

                if not source.is_file():
                    raise RuntimeError(
                        f"Local SVG is missing: {source}"
                    )

                print(f"local {source}")
                svg_path.write_bytes(source.read_bytes())

            else:
                url = BASE_URL + filename
                print(f"download {url}")
                fetch(url, svg_path)

            convert(svg_path, xml_path)
            print(f"created {xml_path}")

        print(f"kept {sepah_png} as PNG; bank_sepah.xml is intentionally not generated")

    finally:
        for path in tmp.glob("*"):
            path.unlink(missing_ok=True)
        tmp.rmdir()


if __name__ == "__main__":
    main()
