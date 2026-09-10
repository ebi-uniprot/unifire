#!/usr/bin/env bash
#
# Checks that params.defaultUnifireVersion in nextflow/conf/defaults.config
# matches the expected UniFIRE version (the Docker image tag derived from a
# v* git tag, a release/* branch or a snapshot/* branch). Used by the
# pre-push git hook and by the release jobs in the CI pipelines.
#
# Usage: check-nextflow-version.sh <expected-version> [<config-file>]
#        check-nextflow-version.sh --config <config-file>
#        check-nextflow-version.sh --tag <tag-or-ref> [--print] [<config-file>]
#
# --tag maps a git tag or branch ref to the corresponding Docker image tag:
#   refs/tags/v1.2.3 or v1.2.3             -> 1.2.3
#   refs/heads/release/v1.2.3              -> 1.2.3
#   refs/heads/release/1.2.3 or release/1.2.3 -> 1.2.3
#   refs/heads/snapshot/v1.2.3             -> 1.2.3-SNAPSHOT
#   refs/heads/snapshot/1.2.3 or snapshot/1.2.3 -> 1.2.3-SNAPSHOT
# With --print the mapped value is printed and no check is run.
#
set -euo pipefail

CONFIG_REL_PATH="nextflow/conf/defaults.config"

usage() {
    echo "Usage: $0 <expected-version> [<config-file>]" >&2
    echo "       $0 --config <config-file>  (verifies value is 'latest' or a version-like string)" >&2
    echo "       $0 --tag <tag-or-ref> [--print] [<config-file>]" >&2
    exit 2
}

error_matches() {
    echo "ERROR: defaultUnifireVersion in ${file} is '${actual}' but expected '${expected}'" >&2
    echo "Update nextflow/conf/defaults.config so that params.defaultUnifireVersion" >&2
    echo "matches the tag or branch name, then re-push." >&2
    exit 1
}

get_value() {
    sed -n 's/.*defaultUnifireVersion *= *"\([^"]*\)".*/\1/p' "$1" | head -n1
}

# Maps a git tag or branch ref to the Docker image tag.
map_tag() {
    local tag="${1#refs/tags/}"
    tag="${tag#refs/heads/}"
    case "$tag" in
        v*) printf '%s' "${tag#v}" ;;
        release/v*) printf '%s' "${tag#release/v}" ;;
        release/*) printf '%s' "${tag#release/}" ;;
        snapshot/v*) printf '%s-SNAPSHOT' "${tag#snapshot/v}" ;;
        snapshot/*) printf '%s-SNAPSHOT' "${tag#snapshot/}" ;;
        *) echo "ERROR: cannot derive image tag from ref '${1}' (expected v* tag, release/* or snapshot/* branch)" >&2; return 1 ;;
    esac
}

file="$CONFIG_REL_PATH"

if [ "${1:-}" = "--config" ]; then
    [ $# -eq 2 ] || { echo "ERROR: --config requires exactly one argument" >&2; usage; }
    file="$2"
    actual="$(get_value "$file")"
    if [ "$actual" = "latest" ]; then
        exit 0
    fi
    if ! printf '%s' "$actual" | grep -Eq '^v?[0-9A-Za-z][0-9A-Za-z._-]*$'; then
        echo "ERROR: defaultUnifireVersion in ${file} is '${actual}' but must be 'latest' or a version-like string (e.g. '1.2.3', '0.1.0-dev1', '1.0.0-SNAPSHOT')" >&2
        exit 1
    fi
    exit 0
fi

if [ "${1:-}" = "--tag" ]; then
    shift
    print_only=0
    positional=""
    file="$CONFIG_REL_PATH"
    for arg in "$@"; do
        if [ "$arg" = "--print" ]; then
            print_only=1
        elif [ -z "$positional" ]; then
            positional="$arg"
        elif [ "$file" = "$CONFIG_REL_PATH" ]; then
            file="$arg"
        else
            usage
        fi
    done
    if [ -z "$positional" ]; then
        echo "ERROR: --tag requires the tag and at most one config file" >&2
        usage
    fi
    expected="$(map_tag "$positional")"
    if [ "$print_only" = "1" ]; then
        printf '%s\n' "$expected"
        exit 0
    fi
    actual="$(get_value "$file")"
    if [ "$actual" != "$expected" ]; then
        error_matches
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
