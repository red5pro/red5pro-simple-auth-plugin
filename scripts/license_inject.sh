#!/bin/bash

# = NOTE =
# Use at root of repository: ./scripts/inject-license.sh
SRC=$(realpath src/main/java)
KIT=$(realpath red5-pro-simpleauth-custom-validator-kit)
STRING="Copyright © 2015 Infrared5"
LICENSE=$(realpath scripts/LICENSE_INJECT)
WAS_UPDATED=0

dirs=( "$SRC" "$KIT" )

for src in "${dirs[@]}"
do
  # check to see if already has license...
  echo "Traversing ${src}..."
  while IFS= read -r -d '' file; do
        if grep -q "$STRING" "$file"; then
                echo "$file"
                echo "Already has license..."
        else
                cat "$LICENSE" "$file" > $$.tmp && mv $$.tmp "$file"
                WAS_UPDATED=1
        fi
  done < <(find "${src}/" -type f -name "*.java" -print0)
done

if [ $WAS_UPDATED != 0 ]; then
  echo "License injection was required. Please commit all updated files."
  exit 1
fi
