# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Runtime data-version resolution for the Nextflow pipeline: `nextflow/versions.nf`
  fetches `versions.json` from GitHub master and merges the bundled
  `nextflow/versions.json` on top of it (in-development versions work from a local
  checkout), falling back to the bundled file when the fetch fails. `--version`
  selects the version set; `--dataVersions FILE` overrides resolution entirely
  (offline/CI). Data versions can now be released independently of workflow releases.
- Enforce that `params.defaultUnifireVersion` in nextflow config matches the version tag
  when a `v*` tag is pushed (shared check script, GitHub Actions and GitLab CI checks,
  local pre-push git hook).
- Publish pre-release Docker images from `snapshot/*` branches: the image is published as
  `<version>-SNAPSHOT`, aligned with the Java versioning of the GitLab library release,
  with the same enforcement chain (never `latest`).
- Support production-style Docker releases from `release/*` branches: `release/v1.0.0` and
  `release/1.0.0` publish `nextflow:1.0.0` (never `latest`), enforced like `v*` tags.

### Changed
- Replace Null positions by "" in output TSV file
- Merge the `v*` tag and `release/*` branch Nextflow Docker build/push jobs into a single
  job using a shared version mapping (`refs/tags/v*`, `refs/heads/release/*`), and stop
  publishing the `latest` Docker tag from CI.

### Removed