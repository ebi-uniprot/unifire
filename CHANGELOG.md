# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Enforce that `params.defaultUnifireVersion` in nextflow config matches the version tag
  when a `v*` tag is pushed (shared check script, GitHub Actions and GitLab CI checks,
  local pre-push git hook).

### Changed
- Replace Null positions by "" in output TSV file

### Removed