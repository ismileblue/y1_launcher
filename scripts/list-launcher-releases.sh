#!/usr/bin/env bash
# List y1_launcher releases that ship an app-release APK.
# Writes JSON array to stdout for GitHub Actions matrix generation.
set -euo pipefail

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "error: missing required command: $1" >&2
        exit 1
    }
}

require_cmd curl
require_cmd python3

python3 - <<'PY'
import json
import os
import sys
import urllib.request

api_url = f"https://api.github.com/repos/{os.environ.get('LAUNCHER_REPO', 'ismileblue/y1_launcher')}/releases?per_page=100"
headers = {
    "Accept": "application/vnd.github+json",
    "User-Agent": "jj-launcher-rom-build/1.0",
}

token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
if token:
    headers["Authorization"] = f"Bearer {token}"

request = urllib.request.Request(api_url, headers=headers)
with urllib.request.urlopen(request) as response:
    releases = json.load(response)

launcher_repo = os.environ.get("LAUNCHER_REPO", "ismileblue/y1_launcher")
items = []
for release in releases:
    if release.get("draft"):
        continue

    apk_assets = [
        asset for asset in release.get("assets", [])
        if asset.get("name", "").startswith("app-release")
        and asset.get("name", "").endswith(".apk")
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
        "launcher_repo": launcher_repo,
    })

items.sort(key=lambda item: item.get("published_at") or item["tag"])
json.dump(items, sys.stdout, ensure_ascii=False)
PY
