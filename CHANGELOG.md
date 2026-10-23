# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New Nextflow-based UniFIRE pipeline as the recommended way to run UniFIRE:
  `nextflow run ebi-uniprot/unifire -r v<version>` runs the rule-execution
  workflows unirule, arba and pirsr on multi-FASTA input (running InterProScan 6
  automatically) or on precomputed InterProScan XML. Rule data is downloaded only
  for the enabled systems and the taxonomy cache is refreshed at run time. The
  pipeline can be exercised with `-stub-run` and ships profiles for docker,
  singularity, podman, apptainer, and batch schedulers (slurm, lsf, awsbatch,
  googlebatch).
- New documentation set under `docs/` (pipeline usage, architecture,
  build-from-source, Docker, input data), replacing the former README walkthrough.
- Self-contained runner script (`docker/scripts/unifire-workflow-nextflow.sh`)
  that runs the pipeline inside the `unifire/nextflow` image on `/volume` inputs,
  preserving the legacy `unifire` image contract.
- Predefined data versions selected with `--version`, resolved at run time from
  upstream (`--dataVersions FILE` overrides this, e.g. offline); rule-data
  versions update independently of pipeline releases.

### Changed
- Input-type inference is now streaming with much lower memory usage, and
  unsupported input types are rejected with a clear error up front.
- Per-process resource budgets by default: 12 GB for unirule/pirsr and
  32 GB for arba rule inference; tunable via the profile configs.
- Rebuilt the `unifire/nextflow` Docker image as a multi-stage build:
  ~1.5 GB instead of ~3.3 GB, faster to pull.
- Rename all Nextflow processes to UPPER_SNAKE_CASE (e.g.
  `RUN_UNIRULE_PIPELINE`); relevant if you override process configuration.
- `--iprscan6ProfileNames` accepts a comma-separated CLI string.
- Replace Null positions by "" in output TSV file

### Removed
- `run_unifire_docker.sh` no longer accepts FASTA input. Use the Nextflow
  pipeline for FASTA, or pass precomputed InterProScan XML (`-t iprscanxml`,
  now the default) to the wrapper.