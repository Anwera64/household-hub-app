#!/usr/bin/env bash
# Usage: release-facts.sh <tag> < tags
#
# Where a new release stands among the repository's tags, read on stdin one per line: bare names,
# or `git ls-remote --tags` lines. Prints two lines for $GITHUB_OUTPUT:
#   latest=true|false   true when no vX.Y.Z tag is higher than <tag>, wherever it was released from
#   notes_start=<tag>   the highest vX.Y.Z tag below <tag>, or empty for the first release
# Versions compare by number (0.10.0 is above 0.9.0). Tags that are not vX.Y.Z are ignored.
set -euo pipefail

tag="${1:?usage: release-facts.sh <tag> < tags}"
version='^v[0-9]+\.[0-9]+\.[0-9]+$'
if ! [[ "$tag" =~ $version ]]; then
  echo "release-facts.sh: $tag is not a vX.Y.Z tag" >&2
  exit 1
fi

# The version tags and the new one, each once, lowest first.
versions="$(
  { awk '{ sub(/^refs\/tags\//, "", $NF); print $NF }'; echo "$tag"; } \
    | grep -E "$version" \
    | sort -u \
    | sort -t . -k1.2,1n -k2,2n -k3,3n
)"

if [ "$(tail -n 1 <<< "$versions")" = "$tag" ]; then
  echo "latest=true"
else
  echo "latest=false"
fi
echo "notes_start=$(awk -v tag="$tag" '$0 == tag { print below; exit } { below = $0 }' <<< "$versions")"
