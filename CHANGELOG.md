# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Runtime data-version resolution for the Nextflow pipeline: `nextflow/versions.nf`
  fetches `versions.json` from GitHub master and merges the bundled
  `nextflow/versions.json` on top of it (in-development versions work from a local
  checkout),   falling back to the bundled file when the fetch fails. `--version`
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
- Accept `--iprscan6ProfileNames` given as a comma-separated CLI string (e.g. `--iprscan6ProfileNames docker`),
  alongside the configuration-provided list, in the InterProScan 6 sub-workflow (a plain String has no `toUnique()`).
- Rebuild `docker/nextflow.Dockerfile` as a multi-stage image: the JDK/Maven/compiler toolchain
  and Python dependencies (ete4, lxml) are installed in a builder stage, and the runtime stage
  ships only a JRE plus the pipeline runtime dependencies. The image measures ~1.5 GB, down from
  ~3.3 GB (~55% reduction), plus a new root `.dockerignore` that excludes build outputs and local
  data from the build context. Runtime dependencies (`git`, `hmmer`, `ncbi-data`) are retained.
- Replace Null positions by "" in output TSV file

### Removed
- `run_unifire_docker.sh` no longer accepts FASTA input (the wrapper would have to run InterProScan 6
  inside the image, which is not practical since all of InterProScan's dependencies would need to be
  installed there). FASTA input is supported by the [Nextflow pipeline](docs/nextflow.md) (recommended),
  where InterProScan 6 runs with its native docker/singularity profiles; the wrapper only accepts
  precomputed InterProScan XML input (`-t iprscanxml`, now the default).