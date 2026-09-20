#!/usr/bin/env python3
"""Verify the 15 Figma sky assets bundled by Tempodroid."""
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SKIES = {
    "figma_sky_day_clear.jpg": 27420,
    "figma_sky_day_partly_cloudy.jpg": 58731,
    "figma_sky_day_cloudy.jpg": 29810,
    "figma_sky_day_rain.jpg": 24779,
    "figma_sky_day_storm.jpg": 38799,
    "figma_sky_sunset_clear.jpg": 27801,
    "figma_sky_sunset_partly_cloudy.jpg": 42479,
    "figma_sky_sunset_cloudy.jpg": 38399,
    "figma_sky_sunset_rain.jpg": 35389,
    "figma_sky_sunset_storm.jpg": 44823,
    "figma_sky_night_clear.jpg": 38214,
    "figma_sky_night_partly_cloudy.jpg": 37337,
    "figma_sky_night_cloudy.jpg": 23044,
    "figma_sky_night_rain.jpg": 34723,
    "figma_sky_night_storm.jpg": 34709,
}
DRAWABLES = ROOT / "app/src/main/res/drawable-nodpi"


def jpeg_dimensions(data: bytes) -> tuple[int, int] | None:
    if len(data) < 4 or data[:2] != b"\xff\xd8" or data[-2:] != b"\xff\xd9":
        return None
    i = 2
    sof = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}
    while i + 3 < len(data):
        if data[i] != 0xFF:
            i += 1
            continue
        while i < len(data) and data[i] == 0xFF:
            i += 1
        if i >= len(data):
            break
        marker = data[i]
        i += 1
        if marker in {0xD8, 0xD9} or 0xD0 <= marker <= 0xD7:
            continue
        if i + 1 >= len(data):
            break
        length = int.from_bytes(data[i:i + 2], "big")
        if length < 2 or i + length > len(data):
            break
        if marker in sof and length >= 7:
            height = int.from_bytes(data[i + 3:i + 5], "big")
            width = int.from_bytes(data[i + 5:i + 7], "big")
            return width, height
        i += length
    return None


def main() -> int:
    failures: list[str] = []
    for name, expected_size in SKIES.items():
        path = DRAWABLES / name
        if not path.is_file():
            failures.append(f"missing: {name}")
            continue
        data = path.read_bytes()
        if len(data) != expected_size:
            failures.append(f"size: {name}={len(data)} expected={expected_size}")
        dimensions = jpeg_dimensions(data)
        if dimensions != (630, 178):
            failures.append(f"dimensions: {name}={dimensions} expected=(630, 178)")

    moon = DRAWABLES / "figma_moon_surface.png"
    if not moon.is_file() or moon.stat().st_size != 151644:
        failures.append("moon texture missing or changed")

    old = sorted(p.name for p in DRAWABLES.glob("figma_sky_*.png"))
    if old:
        failures.append(f"obsolete sky PNGs still bundled: {', '.join(old)}")

    if failures:
        print("Figma visual asset integrity FAILED", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("Figma visual asset integrity OK (15 skies + moon)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
