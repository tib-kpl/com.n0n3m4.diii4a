#!/bin/sh
# Applies this fork's patches to the git submodules it builds but does not own (their repositories
# stay upstream's). Run from the repository root after `git submodule update --init`; the CI does it
# right after the checkout. A patch already applied is left as it is.
#
# patches/<name>-*.patch goes to the submodule below whose folder is <name>.

set -e
cd "$(dirname "$0")/.."

apply() # <submodule path> <patch>
{
	if [ ! -e "$1/.git" ]; then
		echo "skip $2: $1 is not checked out"
	elif git -C "$1" apply --check "$PWD/$2" 2>/dev/null; then
		git -C "$1" apply "$PWD/$2"
		echo "applied $2"
	elif git -C "$1" apply --reverse --check "$PWD/$2" 2>/dev/null; then
		echo "already applied $2"
	else
		echo "error: $2 does not apply to $1" >&2
		exit 1
	fi
}

for p in patches/xash3d-fwgs-*.patch; do [ -e "$p" ] && apply Q3E/src/main/jni/xash3d/xash3d-fwgs "$p"; done
for p in patches/hlsdk-portable-*.patch; do [ -e "$p" ] && apply Q3E/src/main/jni/xash3d/hlsdk-portable "$p"; done
exit 0
