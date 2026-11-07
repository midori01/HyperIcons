#!/usr/bin/env bash
# Prints "<versionName> <versionCode>" for the current checkout.
# Source of truth is the git tag (vX.Y.Z). VERSION_NAME / VERSION_CODE override it.
# Builds past a tag get a describe suffix (6.0.1-2-gabc1234) but keep the tag's code.
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

name="${VERSION_NAME:-}"
if [ -z "$name" ]; then
    name=$(git -C "$DIR" describe --tags --match 'v[0-9]*' 2>/dev/null || true)
fi
name="${name#v}"

if [ -z "$name" ]; then
    echo "[!] No vX.Y.Z tag found; using 0.0.0-dev" >&2
    name="0.0.0-dev"
fi

if [[ ! "$name" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-.*)?$ ]]; then
    echo "[x] Version '$name' is not X.Y.Z" >&2
    exit 1
fi
major=${BASH_REMATCH[1]}
minor=$((10#${BASH_REMATCH[2]}))
patch=$((10#${BASH_REMATCH[3]}))
if [ "$minor" -ge 100 ] || [ "$patch" -ge 100 ]; then
    echo "[x] Version '$name': minor and patch must be below 100" >&2
    exit 1
fi

code="${VERSION_CODE:-$((10#$major * 10000 + minor * 100 + patch))}"
if [ "$code" -lt 1 ]; then
    code=1
fi

echo "$name $code"
