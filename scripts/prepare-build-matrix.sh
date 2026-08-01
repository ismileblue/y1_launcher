#!/usr/bin/env bash
# Build a flattened ROM job matrix from launcher APK releases.
# Usage: prepare-build-matrix.sh <all|new_only>
#
# ryan-specter fork (default):
#   <  Y2_MIN  → APK from ismileblue/y1_launcher → types a,b only
#                release tag: launcher-{tag}  (Y1 ROMs as upstream intended)
#   >= Y2_MIN  → APK from this fork → type y2 only
#                release tag: launcher-{tag}-y2  (separate Y2 release)
#
# ismileblue/y1_launcher (upstream):
#   all tags from self → types a,b
#   >= Y2_MIN also gets type y2
#   release tag: launcher-{tag} with a/b (+ y2 when eligible)
#
# Env:
#   ROM_REPO, Y1_LAUNCHER_REPO, Y2_LAUNCHER_REPO, Y2_MIN_VERSION, GITHUB_REPOSITORY
#
# GITHUB_OUTPUT: has_new, matrix, release_matrix, latest_tag
set -euo pipefail

MODE="${1:-new_only}"
ROM_REPO="${ROM_REPO:-${GITHUB_REPOSITORY:-ryan-specter/jj_auto}}"
Y1_LAUNCHER_REPO="${Y1_LAUNCHER_REPO:-ismileblue/y1_launcher}"
Y2_MIN_VERSION="${Y2_MIN_VERSION:-0.11.2}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-ryan-specter/jj_auto}"

IS_UPSTREAM=0
if [ "$GITHUB_REPOSITORY" = "ismileblue/y1_launcher" ]; then
    IS_UPSTREAM=1
fi

if [ -z "${Y2_LAUNCHER_REPO:-}" ]; then
    if [ "$IS_UPSTREAM" = "1" ]; then
        Y2_LAUNCHER_REPO="ismileblue/y1_launcher"
    else
        Y2_LAUNCHER_REPO="$GITHUB_REPOSITORY"
    fi
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$MODE" != "all" ] && [ "$MODE" != "new_only" ]; then
    echo "usage: $0 <all|new_only>" >&2
    exit 1
fi

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "error: missing required command: $1" >&2
        exit 1
    }
}

require_cmd python3

python3 - "$MODE" "$ROM_REPO" "$Y1_LAUNCHER_REPO" "$Y2_LAUNCHER_REPO" "$Y2_MIN_VERSION" "$IS_UPSTREAM" <<'PY'
import json
import os
import re
import sys
import urllib.request

mode, rom_repo, y1_repo, y2_repo, y2_min, is_upstream_s = sys.argv[1:7]
is_upstream = is_upstream_s == "1"
token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
if not token:
    raise SystemExit("GITHUB_TOKEN is required")

headers = {
    "Authorization": f"Bearer {token}",
    "Accept": "application/vnd.github+json",
    "User-Agent": "jj-launcher-rom-build/1.0",
}


def parse_semver(tag: str):
    raw = tag.strip()
    if raw.startswith("launcher-"):
        raw = raw[len("launcher-") :]
    if raw.endswith("-y2"):
        raw = raw[: -len("-y2")]
    if raw.startswith("v") or raw.startswith("V"):
        raw = raw[1:]
    m = re.match(r"^(\d+)\.(\d+)(?:\.(\d+))?", raw)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3) or 0)


def fetch_apk_releases(repo: str):
    url = f"https://api.github.com/repos/{repo}/releases?per_page=100"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request) as response:
        releases = json.load(response)
    items = []
    for release in releases:
        if release.get("draft"):
            continue
        apk_assets = [
            a for a in release.get("assets", [])
            if a.get("name", "").startswith("app-release") and a.get("name", "").endswith(".apk")
        ]
        if not apk_assets:
            continue
        asset = apk_assets[0]
        items.append({
            "tag": release["tag_name"],
            "name": release.get("name") or release["tag_name"],
            "apk_name": asset["name"],
            "apk_url": asset["browser_download_url"],
            "body": release.get("body") or "",
            "html_url": release.get("html_url", ""),
            "published_at": release.get("published_at", ""),
            "launcher_repo": repo,
        })
    return items

y2_min_tuple = parse_semver(y2_min)
if y2_min_tuple is None:
    raise SystemExit(f"invalid Y2_MIN_VERSION: {y2_min}")

# Use y1_repo as the primary launcher repo
items = fetch_apk_releases(y1_repo)

# Each entry: launcher tag + rom types + release_tag (GitHub release name for the ROMs)
catalog = []

for item in items:
    ver = parse_semver(item["tag"])
    if ver is None:
        continue
    types = ["a", "b"]
    if ver >= y2_min_tuple:
        types.append("y2")
    entry = dict(item)
    entry["types"] = types
    entry["release_tag"] = f"launcher-{item['tag']}"
    entry["include_y2"] = "y2" in types
    entry["y2_only"] = False
    catalog.append(entry)

catalog.sort(key=lambda i: i.get("published_at") or i["tag"])

with open("releases.json", "w", encoding="utf-8") as handle:
    json.dump(catalog, handle, ensure_ascii=False)

existing = set()
page = 1
while True:
    url = f"https://api.github.com/repos/{rom_repo}/releases?per_page=100&page={page}"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request) as response:
        batch = json.load(response)
    if not batch:
        break
    for release in batch:
        tag_name = release.get("tag_name", "")
        if tag_name.startswith("launcher-"):
            existing.add(tag_name)
    page += 1

new_items = [item for item in catalog if item["release_tag"] not in existing]
build_items = catalog if mode == "all" else new_items
has_new = len(build_items) > 0
latest_tag = catalog[-1]["tag"] if catalog else ""

flat = []
for item in build_items:
    for rom_type in item["types"]:
        flat.append({
            "tag": item["tag"],
            "name": item["name"],
            "apk_name": item["apk_name"],
            "apk_url": item["apk_url"],
            "launcher_repo": item["launcher_repo"],
            "type": rom_type,
            "types": item["types"],
            "release_tag": item["release_tag"],
            "y2_only": item["y2_only"],
        })

release_matrix = [
    {
        "tag": item["tag"],
        "name": item["name"],
        "types": item["types"],
        "release_tag": item["release_tag"],
        "include_y2": item["include_y2"],
        "y2_only": item["y2_only"],
        "include_a": "a" in item["types"],
        "include_b": "b" in item["types"],
    }
    for item in build_items
]

mode_label = "upstream(ismileblue)" if is_upstream else "fork(y2-separate)"
print(f"Mode: {mode} ({mode_label})")
print(f"ROM_REPO: {rom_repo}")
print(f"Y1_LAUNCHER_REPO: {y1_repo}")
print(f"Y2_LAUNCHER_REPO: {y2_repo}")
print(f"Y2_MIN_VERSION: {y2_min}")
print(f"Catalog: {[(i['tag'], i['types'], i['release_tag'], i['launcher_repo']) for i in catalog]}")
print(f"Existing launcher-* releases: {sorted(existing)}")
print(f"Build jobs: {[(j['tag'], j['type'], j['release_tag']) for j in flat]}")

github_output = os.environ.get("GITHUB_OUTPUT")
if github_output:
    with open(github_output, "a", encoding="utf-8") as handle:
        handle.write(f"has_new={'true' if has_new else 'false'}\n")
        handle.write(f"matrix={json.dumps(flat)}\n")
        handle.write(f"release_matrix={json.dumps(release_matrix)}\n")
        handle.write(f"latest_tag={latest_tag}\n")

if not has_new:
    print("No ROM builds required.")
PY
