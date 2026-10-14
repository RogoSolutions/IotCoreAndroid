#!/usr/bin/env bash
# Tạo GitHub Release cho một tag đã push và đính kèm file AAR.
# Dùng credential GitHub mà git đang lưu (Git Credential Manager), không cần gh CLI.
#
# Usage: scripts/github-release.sh <tag> <aar_path> [notes]
#   notes: chuỗi JSON-escaped (dùng \n để xuống dòng)
set -euo pipefail

TAG="$1"; AAR="$2"; NOTES="${3:-Release $1}"
REPO="RogoSolutions/IotCoreAndroid"

TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill | sed -n 's/^password=//p')
[ -n "$TOKEN" ] || { echo "Không tìm thấy credential GitHub"; exit 1; }
H=(-H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json")

BODY_FILE=$(mktemp)
trap 'rm -f "$BODY_FILE"' EXIT
printf '{"tag_name":"%s","name":"%s","body":"%s","draft":false,"prerelease":false}' \
  "$TAG" "$TAG" "$NOTES" > "$BODY_FILE"

RESP=$(curl -s "${H[@]}" -H "Content-Type: application/json; charset=utf-8" \
  -X POST "https://api.github.com/repos/$REPO/releases" --data-binary @"$BODY_FILE")
ID=$(echo "$RESP" | sed -n 's/^  "id": \([0-9]*\),$/\1/p' | head -1)
[ -n "$ID" ] || { echo "Tạo release thất bại:"; echo "$RESP" | head -20; exit 1; }

curl -s "${H[@]}" -H "Content-Type: application/octet-stream" --data-binary @"$AAR" \
  "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=IotCoreAndroid-$TAG.aar" \
  | grep -o '"browser_download_url":"[^"]*"'
echo "$RESP" | grep '"html_url"' | head -1
