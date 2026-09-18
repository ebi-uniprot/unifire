# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project Overview

UniFIRE (UniProt Functional annotation Inference Rule Engine) is a rule execution platform that applies rules written in the UniProt Rule Markup Language (URML) to protein sequences, generating automatic annotations from UniProt annotation rules (UniRule, ARBA, PIRSR). It is maintained by EMBL-EBI.

- Maven multi-module Java project (Java 17, Drools rule engine, JAXB-generated models)
- Wrapped by a Nextflow pipeline for reproducible, containerised runs (recommended usage)
- Containerised distribution (Docker image, legacy CLI usage)

Authoritative docs live in `docs/`; prefer linking to them rather than duplicating them in code or other docs.

## Repository Layout

| Path | Purpose |
|------|---------|
| `core` | URML fact/rule model (`uk.ac.ebi.uniprot.urml.core`); JAXB classes generated from `core/src/main/resources/schemas/xsd/urml-facts.xsd` and `urml-rules.xsd`; readers/writers, validation, `FactMerger` |
| `engine` | Drools rule engine (`uk.ac.ebi.uniprot.urml.engine`); URML→Drools translation and rule evaluation |
| `io` | Input parsers (`uk.ac.ebi.uniprot.urml.input`: FASTA headers, InterProScan XML/IPS6, UniParc, Fact XML) and output writers (`uk.ac.ebi.uniprot.urml.output`: TSV, Fact XML); key types `InputType`, `OutputFormat` |
| `procedures` | Rule pre/post procedures, mainly `unirule/positionalfeatures` |
| `distribution` | CLI app (`uk.ac.ebi.uniprot.unifire`); `UniFireApp`, `UniFireRunner`, PIRSR integration; assembles `distribution/bin/unifire.sh`, `pirsr.sh`, `fasta-header-validator.sh` |
| `main.nf` + `nextflow/` | Nextflow pipeline: `nextflow/unifire.nf` (parent `workflow UNIFIRE(run, data, engine)`), `nextflow/data.nf` (data fetching subworkflow), `nextflow/modules/`, `nextflow/conf/profiles*` |
| `samples/` | Sample inputs and rule files downloaded by `build.sh` (unirule/arba/pirsr URML, templates, `pirsr_data/`) |
| `test-data/` | Fixtures and expected outputs for pipeline/Java tests |
| `datadir/` | Cached pipeline-downloaded data (`urml/`, `pirsr/`, `taxa/`) — local artifact, not source |
| `docker/` | `nextflow.Dockerfile`, `versions.properties` (image/rule version tags), wrapper scripts |
| `misc/` | `taxonomy/` python scripts (+ tests), `release/check-nextflow-version.sh`, web docs, media |
| `docs/` | User/dev documentation (see below) |

## Build & Test Commands

Prerequisites: Java 17, Maven ≥ 3.6, (Nextflow ≥ 26.04 and a container engine for the pipeline).

```bash
./build.sh                      # full build: mvn install skipping tests, then downloads URML rules + PIRSR data into samples/
mvn clean verify                # full build WITH tests (what CI runs)
mvn test -pl <module>           # tests for one module, e.g. mvn test -pl engine
python -m unittest discover misc/taxonomy/tests   # taxonomy scripts tests
```

- Tests: JUnit 5 (jupiter), Mockito, Hamcrest; Surefire + Failsafe; JaCoCo coverage.
- Nextflow pipeline smoke tests: `nextflow/tests/smoke.py` (run in the `ghcr.io/ebi-uniprot/unifire/nextflow` container; also exercised via CI `nextflow-smoke-test`).
- Maven profile `jenkins` is EBI-only proxy config; do not use locally.

## Critical Conventions

### Version invariant (enforced — do not break)
`engine.unifireVersion` in `nextflow/defaults.nf` MUST match the release ref:
- `v*` tags and `release/*` branches → exact version
- `snapshot/*` branches → `<version>-SNAPSHOT`

This is verified by the `pre-push` git hook (`.githooks`, wired via `core.hooksPath`), CI (`nextflow-version-check`), and `misc/release/check-nextflow-version.sh`. If you change the version, update `nextflow/defaults.nf` and the pipeline Docker image build (`docker/versions.properties` as applicable). See `docs/release.md` for the full procedure.

### Nextflow strict params boundary
- All `params` reading happens ONLY in `main.nf` (and profile config files).
- Everything downstream is typed: the `UNIFIRE(run, data, engine)` workflow receives strict merges of option maps; processes get values via `val` inputs, not `params`.
- Defaults live as code in `nextflow/defaults.nf` / `nextflow/versions.nf`. Keep this architecture when extending the pipeline (see `docs/architecture.md`).

### Java code
- Java 17; package root `uk.ac.ebi.uniprot.urml.*` (PIRSR group under `org.proteininformationresource.pirsr`).
- Every Java/sh file needs the Apache 2.0 license header: `Copyright (c) 2018 European Molecular Biology Laboratory`.
- Fact/rule model classes are JAXB-generated from XSD — never hand-edit generated classes; only modify the XSDs (and generated classes reflect the schema).
- Lombok is available (used in some engine test fixtures).
- Logging via SLF4J (Logback in runtime).
- Do not add comments unless the surrounding code style warrants it; keep to the existing style.

### Changelog
`CHANGELOG.md` follows Keep-a-Changelog format with semantic versioning — keep an up-to-date `## [Unreleased]` section (Added / Changed / Removed) when making notable changes.

## CI

Both GitLab CI (`.gitlab-ci.yml`) and GitHub Actions (`.github/workflows/ci.yml`) run:
- `build` (`mvn install`), `test` (`mvn clean verify`), `python-test`
- `nextflow-version-check`, `nextflow-smoke-test`
- Docker image build/push jobs (on `v*` / `release/*` / `snapshot/*` refs); Snapshots are pushed with `<version>-SNAPSHOT` tags, never `latest`.

## Documentation Map

| Doc | Contents |
|-----|----------|
| `docs/developer-guide.md` | Fact/rule model, building, execution engine, limitations |
| `docs/architecture.md` | Nextflow params boundary, workflows, strict merge |
| `docs/nextflow.md` | Pipeline user guide: params, stages, container profiles |
| `docs/build-from-source.md` | `build.sh`, `unifire.sh`/`pirsr.sh` CLI usage |
| `docs/release.md` | Release/snapshot procedure and version invariant |
| `docs/input-data.md` | FASTA/XML input formats, InterProScan details |
| `docs/docker.md` | Legacy Docker workflow |

## Local Runtime Artifacts

`datadir/`, `outdir/`, `.unifire/`, `.nextflow.log*`, `__pycache__/` are local outputs — do not commit them; `.gitignore` already covers most. `models/` is a stale scratch directory and is not a Maven module.
