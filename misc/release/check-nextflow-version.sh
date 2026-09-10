#!/usr/bin/env bash
#
# Checks that params.defaultUnifireVersion in nextflow/conf/defaults.config
# matches the expected UniFIRE version (the Docker image tag derived from a
# v* git tag, without the leading "v"). Used by the pre-push git hook and by
# the release jobs in the CI pipelines.
#
# Usage: check-nextflow-version.sh <expected-version> [<config-file>]
#        check-nextflow-version.sh --config <config-file>
#
set -euo pipefail

CONFIG_REL_PATH="nextflow/conf/defaults.config"

usage() {
    echo "Usage: $0 <expected-version> [<config-file>]" >&2
    echo "       $0 --config <config-file>  (verifies value is 'latest' or semver-like)" >&2
    exit 2
}

error_matches() {
    echo "ERROR: defaultUnifireVersion in ${file} is '${actual}' but expected '${expected}'" >&2
    echo "Update nextflow/conf/defaults.config so that params.defaultUnifireVersion" >&2
    echo "matches the tag, then amend the tag." >&2
    exit 1
}

get_value() {
    sed -n 's/.*defaultUnifireVersion *= *"\([^"]*\)".*/\1/p' "$1" | head -n1
}

expected=""
file="$CONFIG_REL_PATH"

if [ "${1:-}" = "--config" ]; then
    [ $# -eq 2 ] || { echo "ERROR: --config requires exactly one argument" >&2; usage; }
    file="$2"
    actual="$(get_value "$file")"
    if [ "$actual" = "latest" ]; then
        exit 0
    fi
    if ! printf '%s' "$actual" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
        echo "ERROR: defaultUnifireVersion in ${file} is '${actual}' but must be 'latest' or '<major>.<minor>.<patch>'" >&2
        exit 1
    fi
    exit 0
fi

[ $# -ge 1 ] && [ $# -le 2 ] || usage
expected="$1"
[ $# -eq 2 ] && file="$2"

actual="$(get_value "$file")"
if [ "$actual" != "$expected" ]; then
    error_matches
fi
exit 0
