# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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