#!/bin/bash

set -e # fail early

VERS="$1"
[ -n "$VERS" ] && VERS="${VERS}_"

function die() {
  echo "Error: " $*
  echo
  echo "USage: $0 <version>"
  echo "Automatically sign [A-Z].apk"
  exit 1
}

[ -z "$1" ] && die

shopt -s extglob  # extended glob pattern

function process() {
	SRC="$1"

	BASE="${SRC/.apk/}"
	
	DATE=`date +%Y%m%d`
	N=1
	while /bin/true; do
		EXT=`python -c "print chr(96+$N)"`
		DEST="${BASE}_${VERS}${DATE}${EXT}.apk"
		[ ! -e "$DEST" ] && break
		N=$((N+1))
		[ "$N" == "27" ] && die "$DEST exists, can't generate higher letter."
	done
	
	ALIAS="${USER/p*/lf}2"
	
	echo "Signing $SRC => $DEST with alias $ALIAS"
	
	jarsigner -verbose -keystore `cygpath -w ~/*-release-*.keystore` "$SRC" $ALIAS
	
	SIZE1=`stat -c "%s" "$SRC"`
	
	for z in ~/{usdk,sdk}/tools/zipalign.exe; do
		if [[ -x "$z" && -e "$SRC" ]]; then
			echo "Using $z"
			"$z" -f -v 4 "$SRC" "$DEST" && rm -v "$SRC"
		fi
	done
	
	[[ -e "$SRC" ]] && mv -v "$SRC" "$DEST"

	SIZE2=`stat -c "%s" "$DEST"`
	
	echo "$DEST has been signed and zipaligned (added $((SIZE2-SIZE1)) bytes)" 
}

for i in [M]+([^_]).apk ; do
	[ -f "$i" ] && process "$i"
done
