#!/usr/bin/env bash
# Copy upstream JJ Launcher release notes verbatim for a ROM release page.
# Usage: format-release-notes.sh <releases.json> <tag> > RELEASE_NOTES.md
set -euo pipefail

RELEASES_JSON="${1:-}"
TAG="${2:-}"

if [ -z "$RELEASES_JSON" ] || [ -z "$TAG" ]; then
    echo "usage: $0 <releases.json> <tag>" >&2
    exit 1
fi

python3 - "$RELEASES_JSON" "$TAG" <<'PY'
import json
import sys

releases_path, tag = sys.argv[1:3]
with open(releases_path, encoding="utf-8") as handle:
    releases = json.load(handle)

match = next((item for item in releases if item["tag"] == tag), None)
if not match:
    raise SystemExit(f"release tag not found: {tag}")

body = match.get("body") or ""
sys.stdout.write(body)
PY
