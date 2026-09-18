#!/usr/bin/env python3
"""Fail fast if the raster assets from the final Figma file are swapped or changed.

The expected Git blob ids below correspond to the original source-image bytes
used by the final DynamicSkyBackground components in Figma. A path mismatch is
therefore detected even when every file is a valid PNG with the right size.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

EXPECTED = {
    "app/src/main/res/drawable-nodpi/figma_sky_clear_day.png": (993364, "423dc248aa98608c7d188376c9da1e130d06a5fb"),
    "app/src/main/res/drawable-nodpi/figma_sky_partly_cloudy.png": (987746, "83e38c7bf8322ae0d1cc44e1a6137e16463cc6f8"),
    "app/src/main/res/drawable-nodpi/figma_sky_cloudy.png": (1051854, "f4e7727444478efd7df04f2877f6f52f42f10597"),
    "app/src/main/res/drawable-nodpi/figma_sky_rain.png": (871409, "d12535fff4a935a657d1c01f57d5cd8412efaa38"),
    "app/src/main/res/drawable-nodpi/figma_sky_storm.png": (909152, "2b077650818b4312d7d264302ac8312b53905871"),
    "app/src/main/res/drawable-nodpi/figma_sky_sunrise.png": (1107678, "e8d3aa2c9739bf53c023069822e3e88843b4fd49"),
    "app/src/main/res/drawable-nodpi/figma_sky_sunset.png": (906815, "ad0c8b764d78ccc9894946389f8afcaa3d46de1e"),
    "app/src/main/res/drawable-nodpi/figma_sky_clear_night.png": (870766, "62155b1d1ec41139eaf7aaa34b5797af7087a75f"),
    "app/src/main/res/drawable-nodpi/figma_sky_cloudy_night.png": (768856, "b96f59303754138b59503f42b476ae153074323f"),
    "app/src/main/res/drawable-nodpi/figma_sky_rain_night.png": (1183653, "ae53a54c75a226da70d94bca2ab37daf3f88c366"),
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
