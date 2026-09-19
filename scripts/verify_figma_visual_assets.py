#!/usr/bin/env python3
"""Fail fast if the raster assets from the canonical Figma file are swapped or changed.

The expected Git blob ids below correspond to the exact source-image bytes used
by the current DynamicSkyBackground variants in Figma. A path mismatch is
therefore detected even when every file is a valid PNG with the right size.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

EXPECTED = {
    "app/src/main/res/drawable-nodpi/figma_sky_clear_day.png": (1031366, "684f66f4078f6af0ffa07ece9d60e860d5f88fef"),
    "app/src/main/res/drawable-nodpi/figma_sky_partly_cloudy.png": (1275595, "55d125c38cfb953001f11bd9858989eaafa1be40"),
    "app/src/main/res/drawable-nodpi/figma_sky_cloudy.png": (1167726, "d6e71e9685fd2ff0d672500e167189b192e0b80e"),
    "app/src/main/res/drawable-nodpi/figma_sky_rain.png": (1144712, "3ad7b750e5a5156d7971144f481842e5d9aa9829"),
    "app/src/main/res/drawable-nodpi/figma_sky_storm.png": (1202370, "91e27aa719f31aaec40051ecec764439bddb0cd1"),
    "app/src/main/res/drawable-nodpi/figma_sky_sunrise.png": (1165101, "cb3d1fd79f2da68f49177f913dfcecb713eaabd9"),
    "app/src/main/res/drawable-nodpi/figma_sky_sunset.png": (1228686, "23b04634567fb7094591b22f513366fc6d69be2b"),
    "app/src/main/res/drawable-nodpi/figma_sky_clear_night.png": (1687599, "b3057916652cbacb5eed70b66029108ad264a1da"),
    "app/src/main/res/drawable-nodpi/figma_sky_cloudy_night.png": (1241609, "08d6d1e87a97597b88c47f12ade0833befb8b1cd"),
    "app/src/main/res/drawable-nodpi/figma_sky_rain_night.png": (1517083, "9f00d46751ad5ebb84d5c0044be70ce949fe97ec"),
    "app/src/main/res/drawable-nodpi/figma_moon_surface.png": (151644, "2e3fa1830c3be5161e773011e3fb0e7ed9a1b395"),
}


def git_blob_id(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def main() -> int:
    failures: list[str] = []
    for relative, (expected_size, expected_blob) in EXPECTED.items():
        path = ROOT / relative
        if not path.is_file():
            failures.append(f"missing: {relative}")
            continue
        data = path.read_bytes()
        actual_blob = git_blob_id(data)
        if len(data) != expected_size or actual_blob != expected_blob:
            failures.append(
                f"mismatch: {relative} size={len(data)} blob={actual_blob} "
                f"expected_size={expected_size} expected_blob={expected_blob}"
            )

    if failures:
        print("Figma visual asset integrity FAILED", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print(f"Figma visual asset integrity OK ({len(EXPECTED)} assets)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
